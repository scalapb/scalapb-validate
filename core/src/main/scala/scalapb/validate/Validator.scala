package scalapb.validate

import io.envoyproxy.pgv.ValidationException
import scala.util.Try

sealed trait Result {
  def isSuccess: Boolean
  def isFailure: Boolean
  def toFailure: Option[Failure]

  def &&(other: => Result): Result =
    (this, other) match {
      case (Success, Success)              => Success
      case (Success, f: Failure)           => f
      case (f: Failure, Success)           => f
      case (Failure(left), Failure(right)) => Failure(left ::: right)
    }
}

object Result {
  def run[T](code: => T): Result =
    Try(code) match {
      case scala.util.Success(_)                      => Success
      case scala.util.Failure(e: ValidationException) => Failure(e :: Nil)
      case scala.util.Failure(ex) =>
        throw new RuntimeException(
          s"Unexpected exception. Please report this as a bug: ${ex.getMessage()}",
          ex
        )
    }

  def apply(cond: => Boolean, onError: => ValidationException): Result =
    if (cond) Success else Failure(onError :: Nil)

  def optional[T](value: Option[T])(eval: T => Result): Result =
    value.fold[Result](Success)(eval)

  def collect(results: Iterable[Result]): Result =
    results.foldLeft[Result](Success) { case (left, right) => left && right }

  def repeated[T](value: Iterable[T])(eval: T => Result): Result =
    collect(value.map(eval))

}

case object Success extends Result {
  def isSuccess: Boolean = true
  def isFailure: Boolean = false
  def toFailure: Option[Failure] = None
}

case class Failure(violations: List[ValidationException]) extends Result {
  def isSuccess: Boolean = false
  def isFailure: Boolean = true
  def toFailure: Option[Failure] = Some(this)
}

object Failure {
  def apply(violation: ValidationException): Failure = Failure(violation :: Nil)
}

trait Validator[T] {
  self =>
  def validate(t: T): Result

  def optional: Validator[Option[T]] =
    new Validator[Option[T]] {
      def validate(t: Option[T]): Result =
        t.fold[Result](Success)(self.validate)
    }
}

object Validator {
  def apply[T: Validator] = implicitly[Validator[T]]
}
