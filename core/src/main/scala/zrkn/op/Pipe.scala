package zrkn.op

import os.{CommandResult, Path, ProcessOutput, SubProcess, proc}

import java.io.*
import java.util.concurrent.ArrayBlockingQueue
import scala.collection.mutable.ListBuffer
import scala.language.{dynamics, implicitConversions}

/**
 * Created by 张武(zhangwu@corp.netease.com) at 2020/9/6
 **/
class Pipe(__cmd: Seq[String], __f: Seq[String] => Seq[String] = identity) extends AbsPipe with Dynamic:
  import Pipe.SubProc
  var __mergeErrorIntoOut = false
  def |[T](next: Pipe.PipeTail[T]) = next.execute(this)
  def selectDynamic(cmd: String): Pipe = __(cmd)
  def >(out: os.ProcessOutput = null, err: os.ProcessOutput = null, merge: java.lang.Boolean = null): this.type =

    if out != null then __stdout = out
    if err != null then __stderr = err
    if merge != null then __mergeErrorIntoOut = merge
    this
  // 执行命令, 所有输出都会直接打出来
  def ! : CommandResult =
    if __stdout == os.Pipe then
      __stdout = os.Inherit
    __.call()
  // 执行命令, 标准输出会捕获到结果中, 如果执行错误会抛出异常
  def !! : String =
    if __stdout == os.Inherit then
      __stdout = os.Pipe
    __.call(check = true).out.text()
  // 执行命令, 所有输出会捕获到结果中
  def !!! : CommandResult =
    if __stdout == os.Inherit then
      __stdout = os.Pipe
    if __stderr == os.Inherit then
      __stderr = os.Pipe
    __.call()
  val __ : Pipe.Ext[Pipe] = new Pipe.Ext[Pipe]:
    val proc: proc = os.proc(__f(__cmd))
    override def apply(cmd: String) = new Pipe(__cmd :+ cmd, __f)
    def spawn: SubProc =
      val prev = __invokePrev
      val sub = proc.spawn(
        stdin = prev.stdout,
        stdout = Pipe.this.__stdout,
        stderr = Pipe.this.__stderr,
        mergeErrIntoOut = __mergeErrorIntoOut,
        cwd = __op_wd
      )
      SubProc(if (__stderr == os.Pipe && __stdout != os.Pipe) sub.stderr else sub.stdout, Pipe.makeCancelCallback(prev, sub))
    override def call(check: Boolean = false): CommandResult =
      val prev = __invokePrev
      proc.call(
        stdin = prev.stdout,
        stdout = Pipe.this.__stdout,
        stderr = Pipe.this.__stderr,
        check = check,
        mergeErrIntoOut = __mergeErrorIntoOut,
        cwd = __op_wd
      )
end Pipe

object Pipe:
  trait Ext[+T <: AbsPipe]:
    def spawn: SubProc
    def call(check: Boolean = false) =
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
      def spawn: SubProc =
        SubProc(new SubProcess.OutputStream(inputStream), () => ())
      override def call(check: Boolean = false): CommandResult =
        val chunks = new ListBuffer[Either[geny.Bytes, geny.Bytes]]()
        os.Internals.transfer0(inputStream, (buf, n) => {chunks.addOne(Left(new geny.Bytes(java.util.Arrays.copyOf(buf, n)))); ()})
        CommandResult("PipeHead" :: name :: Nil, 0, chunks.toSeq)
  def echo(str: String) = new PipeHead(new ByteArrayInputStream(str.getBytes()), "echo")
  def readLine(f: (BufferedReader, Writer) => Unit): AbsPipe =
    new AbsPipe:
      val __ = new Ext[AbsPipe]:
        def spawn: SubProc =
          val cancelCB: ArrayBlockingQueue[() => Unit] = new ArrayBlockingQueue[() => Unit](1)
          val prevProc = __invokePrev
          val po = new PipedOutputStream()
          val writer = if (__stdout == os.Inherit) new OutputStreamWriter(System.out) else OutputStreamWriter(po)
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
    def execute(pipe: Pipe): T
  object bash extends Pipe(Vector.empty, s => Vector("bash", "-c", s.mkString(" "))):
    def apply(cmd: String): Pipe = __(cmd)
  val ! = new Pipe(Vector.empty) with PipeTail[CommandResult]:
    def execute(pipe: Pipe) = pipe.!
  // 直接调用, 并将结果直接输出到控制台, xxx | !#, 或者 xxx.!
  val !# = new PipeTail[CommandResult]:
    def execute(pipe: Pipe) = pipe.!
  // 调用后将结果返回到字符串, 如执行失败会抛出异常
  val !! = new PipeTail[String]:
    def execute(pipe: Pipe) = pipe.!!
  // 调用后将结果封装到CallResult对象
  val !!! = new PipeTail[CommandResult]:
    def execute(pipe: Pipe) = pipe.!!!
end Pipe
