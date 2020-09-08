package zrkn.op

import java.nio.file.NotDirectoryException

import ammonite.ops._

/**
 * Created by 张武(zhangwu@corp.netease.com) at 2020/9/6
 */
trait OuterSession {
  import os.PathConvertible._
  private val inDocker = (root/'rt).toIO.exists
  val rt: Path = if (inDocker) root/'rt else root
  var wd0: Path =
    if (inDocker)
      oPath(sys.env.getOrElse("OUTER_PWD", "/"))
    else pwd
  def oPath(p: String) =
    if (inDocker && !p.matches("^/rt(/.*)?$") && !p.startsWith("/dev/std")) {
      if (p.startsWith("/"))
        Path("/rt" + p, root)
      else Path(p, wd0)
    } else Path(p, pwd)

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
