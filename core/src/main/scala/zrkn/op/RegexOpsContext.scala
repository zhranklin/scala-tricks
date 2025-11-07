package zrkn.op

/**
 * Created by 张武(zhangwu@corp.netease.com) at 2020/9/29
 */

object RegexOpsContext:
  def groups(str: String) = """\(""".r.findAllIn(str).size - """\(\?([idmsuxU=>!:]|<[!=])""".r.findAllIn(str).size - """\\\(""".r.findAllIn(str).size

  class Interped(sc: StringContext):
    def unapplySeq(s: String): Option[Seq[String]] =
      val parts = sc.parts
      val tail = parts.tail.map(s => if (s.startsWith("(")) s else "(.+)" + s)
      val pattern = java.util.regex.Pattern.compile(parts.head + tail.mkString)
      var groupCount = groups(parts.head)
      val usedGroup = tail.map { part =>
        val ret = groupCount
        groupCount += groups(part)
        ret
      }
      val m = pattern matcher s
      if m.matches() then
        Some(usedGroup.map(i => m.group(i + 1)))
      else None

  extension (sc: StringContext) def rr: Interped = new Interped(sc)

  def main(args: Array[String]): Unit =
    1 to 10 foreach { i =>
      "asdf,kkk,.,.,...,a111" match
        case k @ rr"$a,$b(\w+),$c([^a-zA-Z0-9]+)$d(\w\d+)" => println(s"K=$k/A=$a/B=$b/C=$c/D=$d")
    }
    "x(123)y" match
      case rr"$a\($b\)$c" => println(s"A=$a/B=$b/C=$c")
