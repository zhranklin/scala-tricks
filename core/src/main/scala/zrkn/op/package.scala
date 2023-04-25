package zrkn

import zrkn.op.RegexOpsContext.Interped

import java.io.{BufferedReader, Writer}

/**
 * Created by 张武(zhangwu@corp.netease.com) at 2020/9/6
 */
package object op {
  def echo(str: String) = Pipe.echo(str)
  def readLine(f: (BufferedReader, Writer) => Unit): AbsPipe = Pipe.readLine(f)
  val bash = Pipe.bash
  val ! = new Pipe(Vector.empty)
  val callText = Pipe.callText
  val callResult = Pipe.callResult
  val callTerm = Pipe.callTerm

  extension (sc: StringContext) def rr: Interped = new Interped(sc)

}
