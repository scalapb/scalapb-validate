package scalapb.validate

import io.envoyproxy.pgv.ValidationException
import scala.util.Try

sealed trait Result {
  def isSuccess: Boolean
  def isFailure: Boolean
  def toFailure: Option[Failure]
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
  def validate(t: T): Result
}
