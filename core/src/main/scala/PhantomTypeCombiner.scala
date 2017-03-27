/**
 * This is the trait for combining phantomType such as:
 * ```
 *   import PhantomTypeCombiner._
 *   def test[A](implicit ev: ![A <:< String] && [A <:< AnyRef])
 * ```
 * Note: When using these combiners, you should import all symbols in the companion object, or mix in the trait
 */
trait PhantomTypeCombiner {
  trait ![T]
  implicit def $passAnyForNot[T]: ![T] = null
  implicit def $ambiguousTrue[T](implicit ev: T): ![T] = null

  trait &&[T1, T2]
  implicit def $implicitAnd[T1, T2](implicit t1: T2, t2: T2): T1 && T2 = null

  trait ||[T1, T2]
  implicit def $implicitOr1[T1, T2](implicit t1: T1): T1 || T2 = null
  implicit def $implicitOr2[T1, T2](implicit t2: T2, notT1: ![T1]): T1 || T2 = null

}

object PhantomTypeCombiner extends PhantomTypeCombiner
