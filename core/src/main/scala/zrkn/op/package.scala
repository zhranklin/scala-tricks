package zrkn

import os.{Path, PathConvertible}
import zrkn.op.RegexOpsContext.Interped

import java.io.{BufferedReader, Writer}

/**
 * Created by 张武(zhangwu@corp.netease.com) at 2020/9/6
 */
package object op:
  def echo(str: String) = Pipe.echo(str)
  def readLine(f: (BufferedReader, Writer) => Unit): AbsPipe = Pipe.readLine(f)
  val bash = Pipe.bash
  val ! = Pipe.!
  // 直接调用, 并将结果直接输出到控制台
  val !# = Pipe.!#
  // 调用后将结果返回到字符串, 如执行失败会抛出异常
  val !! = Pipe.!!
  // 调用后将结果封装到CallResult对象
  val !!! = Pipe.!!!

  extension (sc: StringContext) def rr: Interped = new Interped(sc)

  extension (s: String)
    def |[T](next: Pipe.PipeTail[T])(using Path) = echo(s) | next
    def |[T <: AbsPipe](next: T): T = echo(s) | next

  private var wd0: Path = os.pwd
  inline given wd: Path = wd0
  def cd[T: PathConvertible](f: T): Unit = wd0 = Path.expandUser(f, wd0)
