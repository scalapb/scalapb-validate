package scalapb.validate.compiler

import com.google.protobuf.duration.Duration
import com.google.protobuf.timestamp.Timestamp

trait Show[T] {
  def apply(v: T): String
}

object Show {
  implicit val showFloat: Show[Float] = (v: Float) => s"${v}f"
  implicit val showDouble: Show[Double] = (v: Double) => s"${v}"
  implicit val showInt: Show[Int] = (v: Int) => v.toString()
  implicit val showLong: Show[Long] = (v: Long) => s"${v}L"
  implicit val showString: Show[String] = (v: String) =>
    StringRulesGen.quoted(v)
  implicit val showTimestamp: Show[Timestamp] = (v: Timestamp) =>
    s"com.google.protobuf.timestamp.Timestamp.of(${v.seconds}L, ${v.nanos})"
  implicit val showDuration: Show[Duration] = (v: Duration) =>
    s"com.google.protobuf.duration.Duration.of(${v.seconds}L, ${v.nanos})"
}
