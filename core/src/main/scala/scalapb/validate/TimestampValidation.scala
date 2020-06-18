package scalapb.validate

import com.google.protobuf.timestamp.Timestamp
import com.google.protobuf.util.Timestamps

object TimestampValidation {
  def currentTimestamp(): Timestamp =
    Timestamp.fromJavaProto(Timestamps.fromMillis(System.currentTimeMillis()))
}
