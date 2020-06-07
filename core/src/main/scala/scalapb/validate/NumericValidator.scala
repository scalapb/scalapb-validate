package scalapb.validate

import io.envoyproxy.pgv.ValidationException
import math.Ordering.Implicits._

object NumericValidator {
  def greaterThan[T](name: String, v: T, limit: T)(implicit
      order: Ordering[T]
  ) =
    Result(
      v > limit,
      new ValidationException(name, v, s"$v must be greater than $limit")
    )

  def lessThan[T](name: String, v: T, limit: T)(implicit order: Ordering[T]) =
    Result(
      v < limit,
      new ValidationException(name, v, s"$v must be less than $limit")
    )

  def greaterThanOrEqual[T](name: String, v: T, limit: T)(implicit
      order: Ordering[T]
  ) =
    Result(
      v >= limit,
      new ValidationException(
        name,
        v,
        s"$v must be greater than or equal to $limit"
      )
    )

  def lessThanOrEqual[T](name: String, v: T, limit: T)(implicit
      order: Ordering[T]
  ) =
    Result(
      v <= limit,
      new ValidationException(
        name,
        v,
        s"$v must be less than or equal to $limit"
      )
    )

  def constant[T](name: String, v: T, limit: T)(implicit order: Ordering[T]) =
    Result(
      v equiv limit,
      new ValidationException(name, v, s"$v must be equal to $limit")
    )
}
