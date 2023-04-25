package zrkn.op

import scala.language.dynamics

/**
 * Created by 张武(zhangwu@corp.netease.com) at 2020/9/6
 */
object Kube {
  class Line(private val line: String, val head: String) extends Dynamic {
    def selectDynamic(field: String): String = {
      val start = head.indexOf(field + " ")
      if (start == -1) ""
      else {
        val end = head.indexWhere(_ != ' ', start + field.length)
        if (end == -1) line.substring(start).trim
        else line.substring(start, end).trim
      }
    }
    override def toString: String = line
  }

  def query(cmd: String)(implicit cwd: os.Path): Vector[Line] = {
    val cmdRes = bash.__(cmd) | callResult
    val lines = cmdRes.out.lines()
    if (lines.isEmpty && cmdRes.err.text().contains("No resource")) {
      Vector()
    } else {
      lines.drop(1).map(new Line(_, lines.head))
    }
  }

  def json(cmd: String)(implicit cwd: os.Path) = {
    val cmdRes = bash.__(cmd + " -ojson") | callResult
    dijon.parse(cmdRes.out.text())
  }

}
