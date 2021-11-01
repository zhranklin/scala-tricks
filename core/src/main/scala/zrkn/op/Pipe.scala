package zrkn.op

import java.io.{BufferedReader, ByteArrayInputStream, InputStreamReader, OutputStreamWriter, PipedInputStream, PipedOutputStream, Writer}
import java.util.concurrent.ArrayBlockingQueue

import os.{CommandResult, Path, ProcessOutput, SubProcess, proc}
import zrkn.op.Pipe.makeCancelCallback

import scala.collection.mutable.ListBuffer
import scala.language.{dynamics, implicitConversions}

abstract class AbsPipe {
  var __prev: Option[AbsPipe] = None
  var __processOutput: ProcessOutput = os.Pipe
  def __print = {
    __processOutput = os.Inherit
    this
  }
  def __setPrev(prev: AbsPipe): AbsPipe = {
    __prev = Some(prev)
    this
  }
  def __setOutput(output: ProcessOutput) = {
    __processOutput = output
    this
  }
  def |(next: AbsPipe) = next.__setPrev(this)
  def |[T](next: Pipe.PipeTail[T]) = next.execute(this)
  def __spawn(wd: os.Path): SubProc
  def __call(implicit wd: os.Path) = {
    val pproc = __spawn(wd)
    proc("cat").call(
      stdin = pproc.stdout
    )
  }
  def __invokePrev(wd: Path): SubProc = __prev.map(_.__spawn(wd)).getOrElse(SubProc(os.Pipe, () => ()))
}

  /**
   * Created by 张武(zhangwu@corp.netease.com) at 2020/9/6
 */
class Pipe(val __cmd: Seq[String], val __f: Seq[String] => Seq[String] = identity, var useStderr: Boolean = false) extends AbsPipe with Dynamic {
  protected val __proc: proc = os.proc(__f(__cmd))
  def __21: Pipe = {
    useStderr = true
    this
  }
  def selectDynamic(cmd: String): Pipe = __(cmd)
  def __(cmd: String): Pipe = new Pipe(__cmd :+ cmd, __f, useStderr)
  def __spawn(wd: os.Path): SubProc = {
    val prev = __invokePrev(wd)
    val (stdout, stderr) = __getOutput
    val proc = __proc.spawn(
      stdin = prev.stdout,
      stdout = stdout,
      stderr = stderr,
      cwd = wd
    )
    __makeSubProc(prev, proc)
  }

  protected var __makeSubProc: (SubProc, SubProcess) => SubProc = (prev, proc) =>  {
    SubProc(if (useStderr) proc.stderr else proc.stdout, makeCancelCallback(prev, proc))
  }
  protected def __getOutput: (ProcessOutput, ProcessOutput) = {
    if (useStderr) {
      (os.Pipe, __processOutput)
    } else {
      (__processOutput, os.Pipe)
    }
  }
}

case class SubProc(stdout: os.ProcessInput, cancelF: () => Unit)

object Pipe {
  class CancelException extends Exception
  def main(args: Array[String]): Unit = {
    println(echo("asdf") | callText)
    println(echo("asdfqq") | !!.cat.`-n` | readLine((r, w) => {
      w.append("qqq\n")
      w.append("ttt\n")
      Iterator.continually(r.readLine()).takeWhile(_ != null).foreach { l =>
        w.append(l + "asdf" + "\n")
      }
    }) | !!.grep.qq | callText)
  }

  class PipeHead(inputStream: java.io.InputStream) extends Pipe(Nil) {
    override def __spawn(wd: os.Path): SubProc = {
      SubProc(new SubProcess.OutputStream(inputStream), () => ())
    }
    override def __call(implicit wd: os.Path): CommandResult = {
      val chunks = new ListBuffer[Either[geny.Bytes, geny.Bytes]]()
      os.Internals.transfer0(inputStream, (buf, n) => chunks.addOne(Left(new geny.Bytes(java.util.Arrays.copyOf(buf, n)))))
      CommandResult(0, chunks.toSeq)
    }
  }
  def echo(str: String) = new PipeHead(new ByteArrayInputStream(str.getBytes()))
  def readLine(f: (BufferedReader, Writer) => Unit): AbsPipe = {
    new AbsPipe {
      override def __setPrev(prev: AbsPipe): AbsPipe = {
        __prev = Some(prev)
        this
      }
      override def __spawn(wd: os.Path): SubProc = {
        val cancelCB: ArrayBlockingQueue[() => Unit] = new ArrayBlockingQueue[() => Unit](1)
        val prevProc = __invokePrev(wd)
        val po = new PipedOutputStream()
        val writer = new OutputStreamWriter(po)
        val pi = new PipedInputStream(po)
        val pproc = proc("cat").spawn(
          stdin = prevProc.stdout,
          stdout = new ProcessOutput {
            def redirectTo = ProcessBuilder.Redirect.PIPE
            def processOutput(out: => SubProcess.OutputStream) = Some {
              () => {
                val rdr = new BufferedReader(new InputStreamReader(out.wrapped))
                try (f(rdr, writer)) catch {
                  case _: CancelException =>
                    writer.close()
                    cancelCB.take()()
                } finally {
                  writer.close()
                }
              }
            }
          }
        )
        cancelCB.add(makeCancelCallback(prevProc, pproc))
        SubProc(pi, cancelCB.peek())
      }
    }
  }
  private def makeCancelCallback(prev: SubProc, proc: SubProcess) = () =>
    try {
      proc.destroy();
      Thread.sleep(100)
    } finally {
      prev.cancelF()
    }

  trait PipeTail[+T] {
    def execute(pipe: AbsPipe): T
  }
  object bash extends Pipe(Vector.empty, s => Vector("bash", "-c", s.mkString(" "))) {
    def apply(cmd: String): Pipe = __(cmd)
  }
  val !! = new Pipe(Vector.empty)
  val callText = new PipeTail[String] {
    def execute(pipe: AbsPipe) = pipe.__call.out.text
  }
  val callResult = new PipeTail[CommandResult] {
    def execute(pipe: AbsPipe) = pipe.__call
  }
}

