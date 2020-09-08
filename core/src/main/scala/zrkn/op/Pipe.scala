package zrkn.op


import java.io.{ByteArrayInputStream, StringBufferInputStream, StringReader}

import os.{CommandResult, Path, ProcessInput, SubProcess}

import scala.collection.mutable.ListBuffer
import scala.language.implicitConversions
import scala.language.dynamics

/**
 * Created by 张武(zhangwu@corp.netease.com) at 2020/9/6
 */
class Pipe(val __cmd: Seq[String], __prev: Option[Pipe], val __f: Seq[String] => Seq[String] = identity) extends Dynamic {
  def __extend(cmd2: Traversable[String]): Pipe = new Pipe(__cmd ++ cmd2, __prev)
  def selectDynamic(name: String): Pipe = __extend(Vector(name))
  val __proc = os.proc(__f(__cmd))
  def |(next: Pipe) = new Pipe(next.__cmd, Some(this), next.__f)
  def |[T](next: Pipe.PipeTail[T]) = next.execute(this)
  protected def __getStdin(wd: Path): ProcessInput = __prev.map(_.__spawn(wd)).getOrElse(os.Pipe)
  protected def __spawn(wd: os.Path): os.ProcessInput = {
    __proc.spawn(
      stdin = __getStdin(wd),
      cwd = wd
    ).stdout
  }
  def __call(implicit wd: os.Path) = {
    __proc.call(
      stdin = __getStdin(wd),
      cwd = wd
    )
  }
}

object Pipe {
  class PipeHead(inputStream: java.io.InputStream) extends Pipe(Nil, None) {
    override protected def __spawn(wd: os.Path): os.ProcessInput = {
      new SubProcess.OutputStream(inputStream)
    }
    override def __call(implicit wd: os.Path): CommandResult = {
      val chunks = new ListBuffer[Either[geny.Bytes, geny.Bytes]]()
      os.Internals.transfer0(inputStream, (buf, n) => chunks.addOne(Left(new geny.Bytes(java.util.Arrays.copyOf(buf, n)))))
      CommandResult(0, chunks.toSeq)
    }
  }
  def echo(str: String) = new PipeHead(new ByteArrayInputStream(str.getBytes()))
  trait PipeTail[+T] {
    def execute(pipe: Pipe): T
  }
  val bash = new Pipe(Vector.empty, None, s => Vector("bash", "-c", s.mkString(" ")))
  val !! = new Pipe(Vector.empty, None)
  val stdout = new PipeTail[String] {
    def execute(pipe: Pipe) = pipe.__call.out.text
  }
}
