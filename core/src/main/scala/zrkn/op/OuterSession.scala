package zrkn.op

import java.nio.file.NotDirectoryException

import ammonite.ops._
import os.PathConvertible._
import zrkn.op.RegexOpsContext._

/**
 * Created by 张武(zhangwu@corp.netease.com) at 2020/9/6
 */
trait OuterSession {
  private val inDocker = (root/'rt).toIO.exists
  private val homePath = sys.env.getOrElse("OUTER_HOME", sys.env.getOrElse("HOME", "~"))
  val rt: Path = if (inDocker) root/'rt else root
  var wd0: Path =
    if (inDocker)
      oPath(sys.env.getOrElse("OUTER_PWD", "/"))
    else pwd
  def oPath(path: String): Path = {
    path.replaceAll("^~", homePath) match {
      case p if !inDocker => Path(p, pwd)
      case rr"/dev/std.*|/rt(/.*)?$p" => Path(p, pwd)
      case rr"/.*$p" => Path("/rt" + p, root)
      case p => Path(p, wd0)
    }
  }

  implicit def wd: Path = wd0

  val cd = (arg: Path) => {
    val realPath = Option(arg)
      .filter(_.isDir)
      .orElse(os.followLink(arg).filter(_.isDir))
    realPath match {
      case None => throw new NotDirectoryException(arg.toString)
      case Some(path) => wd0 = arg; wd0
    }
  }

  implicit def Relativizer[T](p: T)(implicit b: Path, f: T => RelPath): Path = b/f(p)


}
