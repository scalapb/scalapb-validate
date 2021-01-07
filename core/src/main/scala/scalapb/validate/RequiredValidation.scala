package scalapb.validate

import scalapb.GeneratedOneof

object RequiredValidation {
  def apply[T](name: String, value: Option[T]): Result =
    Result(value.nonEmpty, ValidationFailure(name, "None", "is required"))

  def apply[T](name: String, value: GeneratedOneof): Result =
    Result(
      value.isDefined,
      ValidationFailure(name, "Empty", "is required")
    )
}
