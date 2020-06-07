package scalapb.validate

import io.envoyproxy.pgv.ValidationException
import scala.util.Try

sealed trait Result {
  def isSuccess: Boolean
  def isFailure: Boolean
  def toFailure: Option[Failure]

  def &&(other: => Result) = if (isSuccess) other else this
}

object Result {
  def run[T](code: => T): Result =
    (Try[T] {
      code
    }).toOption match {
      case None                          => Success
      case Some(ex: ValidationException) => Failure(ex)
      case Some(ex) =>
        throw new RuntimeException(
          "Unexpected exception. Please report this as a bug"
        )
    }

  def apply(cond: => Boolean, onError: => ValidationException) =
    if (cond) Success else Failure(onError)

  def optional[T](value: Option[T])(eval: T => Result): Result =
    value.fold[Result](Success)(eval)
}

case object Success extends Result {
  def isSuccess: Boolean = true
  def isFailure: Boolean = false
  def toFailure: Option[Failure] = None
}

case class Failure(violation: ValidationException) extends Result {
  def isSuccess: Boolean = false
  def isFailure: Boolean = true
  def toFailure: Option[Failure] = Some(this)
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
  def apply[T](
      cond: T => Boolean,
      onError: T => ValidationException
  ): Validator[T] =
    new Validator[T] {
      def validate(t: T): Result = if (cond(t)) Success else Failure(onError(t))
    }
}
