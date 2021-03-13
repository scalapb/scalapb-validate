package scalapb.validate

import math.Ordering.Implicits._
import com.google.protobuf.timestamp.Timestamp
import com.google.protobuf.duration.Duration

object ComparativeValidation {
  implicit val timestampOrdering: Ordering[Timestamp] = new Ordering[Timestamp] {
    def compare(x: Timestamp, y: Timestamp): Int = {
      val o1 = java.lang.Long.compare(x.seconds, y.seconds)
      if (o1 != 0) o1
      else java.lang.Integer.compare(x.nanos, y.nanos)
    }
  }

  implicit val durationOrdering: Ordering[Duration] = new Ordering[Duration] {
    def compare(x: Duration, y: Duration): Int = {
      val o1 = java.lang.Long.compare(x.seconds, y.seconds)
      if (o1 != 0) o1
      else java.lang.Integer.compare(x.nanos, y.nanos)
    }
  }

  def greaterThan[T: Ordering](name: String, v: T, limit: T) =
    Result(
      v > limit,
      ValidationFailure(name, v, s"$v must be greater than $limit")
    )

  def lessThan[T](name: String, v: T, limit: T)(implicit order: Ordering[T]) =
    Result(
      v < limit,
      ValidationFailure(name, v, s"$v must be less than $limit")
    )

  def greaterThanOrEqual[T: Ordering](name: String, v: T, limit: T) =
    Result(
      v >= limit,
      ValidationFailure(
        name,
        v,
        s"$v must be greater than or equal to $limit"
      )
    )

  def lessThanOrEqual[T: Ordering](name: String, v: T, limit: T) =
    Result(
      v <= limit,
      ValidationFailure(
        name,
        v,
        s"$v must be less than or equal to $limit"
      )
    )

  def rangeGteLte[T: Ordering](name: String, v: T, left: T, right: T) =
    Result(
      (v >= left) && (v <= right),
      ValidationFailure(
        name,
        v,
        s"$v must be in the range [$left, $right]"
      )
    )

  def rangeGteLt[T: Ordering](name: String, v: T, left: T, right: T) =
    Result(
      (v >= left) && (v < right),
      ValidationFailure(
        name,
        v,
        s"$v must be in the range [$left, $right)"
      )
    )

  def rangeGtLte[T: Ordering](name: String, v: T, left: T, right: T) =
    Result(
      (v > left) && (v <= right),
      ValidationFailure(
        name,
        v,
        s"$v must be in the range ($left, $right]"
      )
    )

  def rangeGtLt[T: Ordering](name: String, v: T, left: T, right: T) =
    Result(
      (v > left) && (v < right),
      ValidationFailure(
        name,
        v,
        s"$v must be in the range ($left, $right)"
      )
    )

  def rangeGteLteEx[T: Ordering](name: String, v: T, right: T, left: T) =
    Result(
      (v >= right) || (v <= left),
      ValidationFailure(
        name,
        v,
        s"$v must be outside the range ($left, $right)"
      )
    )

  def rangeGteLtEx[T: Ordering](name: String, v: T, right: T, left: T) =
    Result(
      (v >= right) || (v < left),
      ValidationFailure(
        name,
        v,
        s"$v must be outside the range [$left, $right)"
      )
    )

  def rangeGtLteEx[T: Ordering](name: String, v: T, right: T, left: T) =
    Result(
      (v > right) || (v <= left),
      ValidationFailure(
        name,
        v,
        s"$v must be outside the range ($left, $right]"
      )
    )

  def rangeGtLtEx[T: Ordering](name: String, v: T, right: T, left: T) =
    Result(
      (v > right) || (v < left),
      ValidationFailure(
        name,
        v,
        s"$v must be outside the range [$left, $right]"
      )
    )

  def constant[T](name: String, v: T, limit: T)(implicit order: Ordering[T]) =
    Result(
      v equiv limit,
      ValidationFailure(name, v, s"$v must be equal to $limit")
    )
}
