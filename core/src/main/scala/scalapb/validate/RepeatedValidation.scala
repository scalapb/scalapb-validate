package scalapb.validate

import io.envoyproxy.pgv.ValidationException

object RepeatedValidation {
  def maxItems[T](name: String, v: Seq[T], limit: Int): Result =
    maxItems(name, v.size, limit)

  def minItems[T](name: String, v: Seq[T], limit: Int): Result =
    minItems(name, v.size, limit)

  def maxItems(name: String, actualSize: Int, limit: Int): Result =
    Result(
      actualSize <= limit,
      new ValidationException(
        name,
        actualSize,
        s"Expected at most $limit elements, got $actualSize"
      )
    )

  def minItems(name: String, actualSize: Int, limit: Int): Result =
    Result(
      actualSize >= limit,
      new ValidationException(
        name,
        actualSize,
        s"$actualSize must have at least $limit elements, got $actualSize"
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
