package scalapb.validate

import io.envoyproxy.pgv.ValidationException
import math.Ordering.Implicits._

object NumericValidator {
  def greaterThan[T: Ordering](name: String, v: T, limit: T) =
    Result(
      v > limit,
      new ValidationException(name, v, s"$v must be greater than $limit")
    )

  def lessThan[T](name: String, v: T, limit: T)(implicit order: Ordering[T]) =
    Result(
      v < limit,
      new ValidationException(name, v, s"$v must be less than $limit")
    )

  def greaterThanOrEqual[T: Ordering](name: String, v: T, limit: T) =
    Result(
      v >= limit,
      new ValidationException(
        name,
        v,
        s"$v must be greater than or equal to $limit"
      )
    )

  def lessThanOrEqual[T: Ordering](name: String, v: T, limit: T) =
    Result(
      v <= limit,
      new ValidationException(
        name,
        v,
        s"$v must be less than or equal to $limit"
      )
    )

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

  def rangeGteLte[T: Ordering](name: String, v: T, left: T, right: T) =
    Result(
      (v >= left) && (v <= right),
      new ValidationException(
        name,
        v,
        s"$v must be in the range [$left, $right]"
      )
    )

  def rangeGteLt[T: Ordering](name: String, v: T, left: T, right: T) =
    Result(
      (v >= left) && (v < right),
      new ValidationException(
        name,
        v,
        s"$v must be in the range [$left, $right)"
      )
    )

  def rangeGtLte[T: Ordering](name: String, v: T, left: T, right: T) =
    Result(
      (v > left) && (v <= right),
      new ValidationException(
        name,
        v,
        s"$v must be in the range ($left, $right]"
      )
    )

  def rangeGtLt[T: Ordering](name: String, v: T, left: T, right: T) =
    Result(
      (v > left) && (v < right),
      new ValidationException(
        name,
        v,
        s"$v must be in the range ($left, $right)"
      )
    )

  def rangeGteLteEx[T: Ordering](name: String, v: T, right: T, left: T) =
    Result(
      (v >= right) || (v <= left),
      new ValidationException(
        name,
        v,
        s"$v must be outside the range ($left, $right)"
      )
    )

  def rangeGteLtEx[T: Ordering](name: String, v: T, right: T, left: T) =
    Result(
      (v >= right) || (v < left),
      new ValidationException(
        name,
        v,
        s"$v must be outside the range [$left, $right)"
      )
    )

  def rangeGtLteEx[T: Ordering](name: String, v: T, right: T, left: T) =
    Result(
      (v > right) || (v <= left),
      new ValidationException(
        name,
        v,
        s"$v must be outside the range ($left, $right]"
      )
    )

  def rangeGtLtEx[T: Ordering](name: String, v: T, right: T, left: T) =
    Result(
      (v > right) || (v < left),
      new ValidationException(
        name,
        v,
        s"$v must be outside the range [$left, $right]"
      )
    )

  def constant[T](name: String, v: T, limit: T)(implicit order: Ordering[T]) =
    Result(
      v equiv limit,
      new ValidationException(name, v, s"$v must be equal to $limit")
    )
}
