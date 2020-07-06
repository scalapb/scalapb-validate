package scalapb.validate

import io.envoyproxy.pgv.ValidationException
import scalapb.GeneratedEnum

object EnumValidation {
  def definedOnly[T <: GeneratedEnum](name: String, value: T): Result =
    Result(
      !value.isUnrecognized,
      new ValidationException(name, value.value, "must be defined")
    )
}
