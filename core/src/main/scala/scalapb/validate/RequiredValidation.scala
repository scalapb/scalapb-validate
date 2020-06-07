package scalapb.validate

import io.envoyproxy.pgv.ValidationException

object RequiredValidation {
  def apply[T](name: String, value: Option[T]): Result =
    Result(value.nonEmpty, new ValidationException(name, "None", "is required"))
}
