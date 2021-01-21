package scalapb.validate

object MembershipValidation {
  def in[T](name: String, v: T, values: Seq[T]) =
    Result(
      values.contains(v),
      ValidationFailure(
        name,
        v,
        s"""$v must be in ${values.mkString("[", ", ", "]")}""""
      )
    )

  def notIn[T](name: String, v: T, values: Seq[T]) =
    Result(
      !values.contains(v),
      ValidationFailure(
        name,
        v,
        s"""$v must not be in ${values.mkString("[", ", ", "]")}""""
      )
    )
}
