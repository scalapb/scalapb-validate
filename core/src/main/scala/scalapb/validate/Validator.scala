package scalapb.validate

import io.envoyproxy.pgv
import scala.util.Try
import com.google.protobuf.InvalidProtocolBufferException

sealed trait Result {
  def isSuccess: Boolean
  def isFailure: Boolean
  def toFailure: Option[Failure]

  def &&(other: Result): Result =
    (this, other) match {
      case (Success, Success)              => Success
      case (Success, f: Failure)           => f
      case (f: Failure, Success)           => f
      case (Failure(left), Failure(right)) => Failure(left ::: right)
    }

  def ||(other: => Result): Result =
    (this, other) match {
      case (Success, _)        => Success
      case (Failure(_), right) => right
    }
}

/** Represents a failure to validate a single field */
case class ValidationFailure(field: String, value: Any, reason: String) {
  override def toString: String = s"$field: $reason - Got $value"

  // Provide pgv.ValidaitonException methods for source code compatibility:
  @deprecated("Use 'field' instead.", "0.2.0")
  def getField(): String = field

  @deprecated("Use 'value' instead.", "0.2.0")
  def getValue(): Any = value

  @deprecated("Use 'reason' instead.", "0.2.0")
  def getReason(): String = reason
}

object Result {
  def run[T](code: => T): Result =
    Try(code) match {
      case scala.util.Success(_) => Success
      case scala.util.Failure(e: pgv.ValidationException) =>
        Failure(
          new ValidationFailure(
            e.getField(),
            e.getValue(),
            e.getReason()
          ) :: Nil
        )
      case scala.util.Failure(ex) =>
        throw new RuntimeException(
          s"Unexpected exception. Please report this as a bug: ${ex.getMessage()}",
          ex
        )
    }

  def apply(cond: => Boolean, onError: => ValidationFailure): Result =
    if (cond) Success else Failure(onError :: Nil)

  def optional[T](value: Option[T])(eval: T => Result): Result =
    value.fold[Result](Success)(eval)

  def collect(results: Iterator[Result]): Result =
    results.foldLeft[Result](Success) { case (left, right) => left && right }

  def collect(results: Iterable[Result]): Result = collect(results.iterator)

  def repeated[T](value: Iterator[T])(eval: T => Result): Result =
    collect(value.map(eval))

  def repeated[T](value: Iterable[T])(eval: T => Result): Result =
    repeated(value.iterator)(eval)
}

case object Success extends Result {
  def isSuccess: Boolean = true
  def isFailure: Boolean = false
  def toFailure: Option[Failure] = None
}

case class Failure(violations: List[ValidationFailure]) extends Result {
  def isSuccess: Boolean = false
  def isFailure: Boolean = true
  def toFailure: Option[Failure] = Some(this)
}

object Failure {
  def apply(violation: ValidationFailure): Failure = Failure(violation :: Nil)
}

trait Validator[T] extends Serializable {
  self =>
  def validate(t: T): Result

  def optional: Validator[Option[T]] =
    new Validator[Option[T]] {
      def validate(t: Option[T]): Result =
        t.fold[Result](Success)(self.validate)
    }
}

class ValidationException(message: String)
    extends InvalidProtocolBufferException(message)

class FieldValidationException(failure: Failure)
    extends ValidationException(
      "Validation failed: " + failure.violations
        .map(_.toString())
        .mkString(", ")
    ) {}

object Validator {
  def apply[T: Validator] = implicitly[Validator[T]]

  def assertValid[T: Validator](instance: T): Unit =
    Validator[T].validate(instance) match {
      case Success =>
      case failure: Failure =>
        throw new FieldValidationException(failure)
    }
}
