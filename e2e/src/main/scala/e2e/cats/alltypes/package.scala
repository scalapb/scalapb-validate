package e2e.cats

import e2e.cats.types.{Color, SubMsg}
import com.google.protobuf.ByteString

package object alltypes {
  implicit val subMsg = Ordering.fromLessThan[SubMsg]((x, y) => x.a < y.a)

  implicit val color = Ordering.fromLessThan[Color]((x, y) => x.value < y.value)

  implicit val byteString: Ordering[ByteString] =
    Ordering.String.on[ByteString](_.toByteArray.toVector.mkString("-"))

  implicit val kernelOrderSubMsg = cats.kernel.Order.fromOrdering(subMsg)

  implicit val kernelOrderColor = cats.kernel.Order.fromOrdering(color)

  implicit val kernelOrderByteString =
    cats.kernel.Order.fromOrdering(byteString)
}
