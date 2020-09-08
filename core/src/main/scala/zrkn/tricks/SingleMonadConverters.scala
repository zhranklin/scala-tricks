package zrkn.tricks

import scala.concurrent.Future
import scala.util.{Failure, Try}

/**
 * Convert Option to Future or Try
 */
trait SingleMonadConverters {

  implicit class OptionToFuture[T](opt: Option[T]) {
    def toFuture = opt match {
      case Some(v) ⇒ Future.successful(v)
      case _ ⇒ Future.failed(SingleMonadConverters.NoException)
    }
  }

  implicit class OptionToTry[T](opt: Option[T]) {
    def toFuture = opt match {
      case Some(v) ⇒ Try(v)
      case _ ⇒ Failure(SingleMonadConverters.NoException)
    }
  }

}

object SingleMonadConverters extends SingleMonadConverters {
  object NoException extends RuntimeException
}
