package zrkn.op

/**
 * Created by 张武(zhangwu@corp.netease.com) at 2020/9/29
 */
trait RegexOpsContext {
  implicit class RegexOps(sc: StringContext) {
    import java.util.regex.Pattern
    private def groups(str: String) = "\\(".r.findAllIn(str).size - """\(\?([idmsuxU=>!:]|<[!=])""".r.findAllIn(str).size
    def rr: Interped = new Interped
    class Interped {
      def unapplySeq(s: CharSequence): Option[Seq[String]] = {
        val parts = sc.parts
        val tail = parts.tail.map(s => if (s.startsWith("(")) s else "(.*)" + s)
        val pattern = Pattern.compile(parts.head + tail.mkString)
        var groupCount = groups(parts.head)
        val usedGroup = tail.map(part => {
          val ret = groupCount
          groupCount += groups(part)
          ret
        })
        val m = pattern matcher s
        if (m.matches()) {
          Some(usedGroup.map(i => m.group(i + 1)))
        } else None
      }
    }
  }
}

object RegexOpsContext extends RegexOpsContext {
  def main(args: Array[String]): Unit = {
    1 to 10 foreach { i =>
      "asdf,kkk,.,.,...,a" match {
        case k @ rr"$a,$b(\w+)$d" => println(a); println(b); println(k); println(d)
      }
    }
  }
}
