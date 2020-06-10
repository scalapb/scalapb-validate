package scalapb.validate

import io.envoyproxy.pgv.ValidationException

object RepeatedValidation {
  def maxItems[T](name: String, v: Seq[T], limit: Int) =
    Result(
      v.size <= limit,
      new ValidationException(
        name,
        v,
        s"$v must have at most $limit elements"
      )
    )

  def minItems[T](name: String, v: Seq[T], limit: Int) =
    Result(
      v.size >= limit,
      new ValidationException(
        name,
        v,
        s"$v must have at least $limit elements"
      )
    )

  def unique[T](name: String, v: Seq[T]) =
    Result(
      v.distinct.size == v.size,
      new ValidationException(
        name,
        v,
        s"$v must have distinct elements"
      )
    )
}
