package e2e.cats

import cats.kernel.Order
import e2e.cats.types.{Color, SubMsg}
import com.google.protobuf.ByteString

package object alltypes {
  implicit val subMsg: Ordering[SubMsg] =
    Ordering.fromLessThan[SubMsg]((x, y) => x.a < y.a)

  implicit val color: Ordering[Color] =
    Ordering.fromLessThan[Color]((x, y) => x.value < y.value)

  implicit val byteString: Ordering[ByteString] =
    Ordering.String.on[ByteString](_.toByteArray.toVector.mkString("-"))

  implicit val kernelOrderSubMsg: Order[SubMsg] =
    cats.kernel.Order.fromOrdering(subMsg)

  implicit val kernelOrderColor: Order[Color] =
    cats.kernel.Order.fromOrdering(color)

  implicit val kernelOrderByteString: Order[ByteString] =
    cats.kernel.Order.fromOrdering(byteString)
}
