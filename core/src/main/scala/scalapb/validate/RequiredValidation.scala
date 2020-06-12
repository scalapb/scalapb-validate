package scalapb.validate

import io.envoyproxy.pgv.ValidationException
import scalapb.GeneratedOneof

object RequiredValidation {
  def apply[T](name: String, value: Option[T]): Result =
    Result(value.nonEmpty, new ValidationException(name, "None", "is required"))

  def apply[T](name: String, value: GeneratedOneof): Result =
    Result(value.isDefined, new ValidationException(name, "Empty", "is required"))
}
