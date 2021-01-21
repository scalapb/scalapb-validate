package scalapb.validate

import scalapb.GeneratedEnum

object EnumValidation {
  def definedOnly[T <: GeneratedEnum](name: String, value: T): Result =
    Result(
      !value.isUnrecognized,
      ValidationFailure(name, value.value, "must be defined")
    )
}
