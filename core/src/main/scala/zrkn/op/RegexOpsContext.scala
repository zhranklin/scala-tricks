package zrkn.op

/**
 * Created by 张武(zhangwu@corp.netease.com) at 2020/9/29
 */
trait RegexOpsContext {
  implicit class RegexOps(sc: StringContext) {
    import java.util.regex.Pattern
    private def groups(str: String) = "\\(".r.findAllIn(str).size - """\(\?([idmsuxU=>!:]|<[!=])""".r.findAllIn(str).size
    private def doUnapply(pattern: Pattern, head: String, tail: List[String], s: CharSequence) = {
      if (tail.exists(!_.startsWith("("))) {
        throw new IllegalArgumentException("Format error, variables should be followed by '(' or at the end. And there should not be duplicate variables.")
      }
      var totalGroups = groups(head)
      val usedGroup = tail.map(part => {
        val ret = totalGroups
        totalGroups += groups(part)
        ret
      })
      val m = pattern matcher s
      if (m.matches()) {
        Some(usedGroup.map(i => m.group(i + 1)))
      } else None
    }
    def rr: scala.util.matching.Regex = {
      val matchF: (Pattern, CharSequence) => Option[List[String]] = sc.parts.toList match {
        case Nil | _ :: Nil => (pattern, s) =>
          if (pattern.matcher(s).matches()) Some(Nil) else None
        case head :: tail if tail.last == "" => (pattern, s) =>
          doUnapply(pattern, head, tail.dropRight(1), s).map(_ ::: List(s.toString))
        case head :: tail => (pattern, s) =>
          doUnapply(pattern, head, tail, s)
      }
      new scala.util.matching.Regex(sc.parts.mkString, List.tabulate(groups(sc.parts.mkString)){_ => "x"}: _*) {
        override def unapplySeq(s: CharSequence): Option[List[String]] = matchF(pattern, s)
      }
    }
  }
}

object RegexOpsContext extends RegexOpsContext
