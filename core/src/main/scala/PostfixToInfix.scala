/**
 * An alternative syntax to postfix:
 * ```
 * aJavaList asScala <> map (_ toString <>)
 * ```
 *
 * When implicit parameter list is encountered, use <>.i instead, suppose `a(implicit y: Y)` and `b` is methods of the object x
 *
 * ```
 * x a <>.i b <>
 * ```
 * Note: When using `<>`, you should import all symbols in the companion object, or mix in the trait
 */
trait PostfixToInfix {

  object <> {
    def i[I](implicit i: I) = i
  }

  implicit class Infix[A](o: A) {
    def apply(i: <>.type) = o
  }

}

object PostfixToInfix extends PostfixToInfix
