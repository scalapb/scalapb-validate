package scalapb.validate

import io.envoyproxy.pgv.ValidationException

object MembershipValidation {
  def in[T](name: String, v: T, values: Seq[T]) =
    Result(
      values.contains(v),
      new ValidationException(
        name,
        v,
        s"""$v must be in ${values.mkString("[", ", ", "]")}""""
      )
    )

  def notIn[T](name: String, v: T, values: Seq[T]) =
    Result(
      !values.contains(v),
      new ValidationException(
        name,
        v,
        s"""$v must not be in ${values.mkString("[", ", ", "]")}""""
      )
    )
}
