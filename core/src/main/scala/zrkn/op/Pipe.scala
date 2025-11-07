package zrkn.op

import os.ProcessInput.SourceInput
import os.{CommandResult, Path, ProcessOutput, SubProcess, proc}
import zrkn.op.Pipe.SubProc

import java.io.*
import java.util.concurrent.ArrayBlockingQueue
import scala.collection.mutable.ListBuffer
import scala.language.{dynamics, implicitConversions}

/**
 * Created by 张武(zhangwu@corp.netease.com) at 2020/9/6
 **/
class Pipe(__cmd: Seq[String], __f: Seq[String] => Seq[String] = identity, private var useStderr: Boolean = false) extends AbsPipe with Dynamic:
  import Pipe.SubProc
  def |[T](next: Pipe.PipeTail[T])(using Path) = next.execute(this)
  def selectDynamic(cmd: String): Pipe = __(cmd)
  def !(using Path): CommandResult = this | Pipe.!#
  def !!(using Path): String = this | Pipe.!!
  def !!!(using Path): CommandResult = this | Pipe.!!!
  val __ : Pipe.Ext[Pipe] = new Pipe.Ext[Pipe]:
    def getOutput: (ProcessOutput, ProcessOutput) =
      if useStderr then
        (os.Pipe, __processOutput)
      else
        (__processOutput, os.Pipe)
    val proc: proc = os.proc(__f(__cmd))
    override def apply(cmd: String) = new Pipe(__cmd :+ cmd, __f, useStderr)
    def spawn(using wd: Path): SubProc =
      val prev = __invokePrev
      val (stdout, stderr) = getOutput
      val sub = proc.spawn(
        stdin = prev.stdout,
        stdout = stdout,
        stderr = stderr,
        cwd = wd
      )
      SubProc(if (useStderr) sub.stderr else sub.stdout, Pipe.makeCancelCallback(prev, sub))
    override def call(check: Boolean = false)(using wd: Path): CommandResult =
      val prev = __invokePrev
      val (stdout, stderr) = getOutput
      proc.call(
        stdin = prev.stdout,
        stdout = stdout,
        stderr = stderr,
        check = check,
        cwd = wd
      )
end Pipe

object Pipe:
  trait Ext[+T <: AbsPipe]:
    def spawn(using Path): SubProc
    def call(check: Boolean = false)(using Path) =
      val pproc = spawn
      proc("cat").call(
        stdin = pproc.stdout,
        check = check,
      )

    def apply(cmd: String): T = ???
  case class SubProc(stdout: os.ProcessInput, cancelF: () => Unit)
  class CancelException extends Exception
  def main(args: Array[String]): Unit =
    println(!.echo.__invokePrev | !!)
    println(echo("asdf") | !!)
    println(echo("asdfqq\n111cccqq") | !.cat.`-n` | readLine{ (r, w) =>
      w.append("aaa\n")
      w.append("bbbqq\n")
      Iterator.continually(r.readLine()).takeWhile(_ != null).foreach { l =>
        w.append(l + "asdf" + "\n")
      }
      w.append("ccc\n")
      w.append("dddqq\n")
    } | !.grep.qq | !!)
    val sub1 = bash.__("sleep 1; echo ba; sleep 1; echo b; sleep 1; echo c")
    val sub2 = readLine{ (r, w) =>
      w.append("qqq\n")
      w.append("ttt\n")
      Iterator.continually(r.readLine()).takeWhile(_ != null).foreach { l =>
        w.append(l + "asdf" + "\n")
      }
    }
    sub1 | sub2 | !.cat | !#
    bash.__("sleep 1; echo ba; sleep 1; echo b; sleep 1; echo c") | !#
    println("asdfasdfasdf")
  end main

  class PipeHead(inputStream: java.io.InputStream, name: String) extends Pipe(Nil):
    override val __ = new Ext[PipeHead]:
      def spawn(using Path): SubProc =
        SubProc(new SubProcess.OutputStream(inputStream), () => ())
      override def call(check: Boolean = false)(using Path): CommandResult =
        val chunks = new ListBuffer[Either[geny.Bytes, geny.Bytes]]()
        os.Internals.transfer0(inputStream, (buf, n) => {chunks.addOne(Left(new geny.Bytes(java.util.Arrays.copyOf(buf, n)))); ()})
        CommandResult("PipeHead" :: name :: Nil, 0, chunks.toSeq)
  def echo(str: String) = new PipeHead(new ByteArrayInputStream(str.getBytes()), "echo")
  def readLine(f: (BufferedReader, Writer) => Unit): AbsPipe =
    new AbsPipe:
      val __ = new Ext[AbsPipe]:
        def spawn(using Path): SubProc =
          val cancelCB: ArrayBlockingQueue[() => Unit] = new ArrayBlockingQueue[() => Unit](1)
          val prevProc = __invokePrev
          val po = new PipedOutputStream()
          val writer = if (__processOutput == os.Inherit) new OutputStreamWriter(System.out) else OutputStreamWriter(po)
          val pi = new PipedInputStream(po)
          val pproc = proc("cat").spawn(
            stdin = prevProc.stdout,
            stdout = new ProcessOutput:
              def redirectTo = ProcessBuilder.Redirect.PIPE
              def processOutput(out: => SubProcess.OutputStream) = Some { () =>
                val rdr = new BufferedReader(new InputStreamReader(out.wrapped))
                try f(rdr, writer)
                catch
                  case _: CancelException =>
                    writer.close()
                    cancelCB.take()()
                finally
                  writer.close()
              }
          )
          cancelCB.add(makeCancelCallback(prevProc, pproc))
          SubProc(pi, cancelCB.peek())
        end spawn
  end readLine
  private def makeCancelCallback(prev: SubProc, proc: SubProcess) = () =>
    try
      proc.destroy();
      Thread.sleep(100)
    finally
      prev.cancelF()

  trait PipeTail[+T]:
    def execute(pipe: Pipe)(using Path): T
  object bash extends Pipe(Vector.empty, s => Vector("bash", "-c", s.mkString(" "))):
    def apply(cmd: String): Pipe = __(cmd)
  val ! = new Pipe(Vector.empty)
  // 直接调用, 并将结果直接输出到控制台
  val !# = new PipeTail[CommandResult]:
    def execute(pipe: Pipe)(using Path) = pipe.__setOutput(os.Inherit).__.call()
  // 调用后将结果返回到字符串, 如执行失败会抛出异常
  val !! = new PipeTail[String]:
    def execute(pipe: Pipe)(using Path) = pipe.__.call(check = true).out.text()
  // 调用后将结果封装到CallResult对象
  val !!! = new PipeTail[CommandResult]:
    def execute(pipe: Pipe)(using Path) = pipe.__.call()
end Pipe
