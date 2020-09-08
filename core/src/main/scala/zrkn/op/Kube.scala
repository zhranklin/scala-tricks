package zrkn.op

import io.circe.Json

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
  }

  def query(cmd: String)(implicit cwd: os.Path): Vector[Line] = {
    val cmdRes = ammonite.ops.%%("bash", "-c", cmd)(cwd)
    val lines = cmdRes.out.lines
    if (lines.isEmpty && cmdRes.err.string.contains("No resource")) {
      Vector()
    } else {
      lines.drop(1).map(new Line(_, lines.head))
    }
  }

  def yaml(cmd: String)(implicit cwd: os.Path): Json = {
    val cmdRes = ammonite.ops.%%("bash", "-c", cmd)(cwd)
    io.circe.yaml.parser.parse(cmdRes.out.string) match {
      case Right(value) => value
      case Left(err) => System.err.println(err.message); Json.Null
    }
  }

}
