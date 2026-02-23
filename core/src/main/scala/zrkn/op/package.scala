package zrkn

import os.{Path, PathConvertible}
import zrkn.op.RegexOpsContext.Interped

import java.io.{BufferedReader, Writer}

/**
 * Created by 张武(zhangwu@corp.netease.com) at 2020/9/6
 */
package object op:
  export FileLock.withFileLock, Pipe.{echo, readLine, bash, !, !!, !!!}

// 直接调用, 并将结果直接输出到控制台, !和!#一样, 只是scala 2.x好像用不了!, 此时用!#替代
  val !# = Pipe.!#

  extension (sc: StringContext) def rr: Interped = new Interped(sc)

  extension (s: String)
    def |[T](next: Pipe.PipeTail[T] with AbsPipe) = echo(s) | next.asInstanceOf[Pipe.PipeTail[T]]
    def |[T](next: Pipe.PipeTail[T]) = echo(s) | next
    def |[T <: AbsPipe](next: T): T = echo(s) | next

  val cd = os.dynamicPwd.withValue
