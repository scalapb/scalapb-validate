package scalapb.validate.transforms

import scalapb.validate.ValidationHelpers
import scalapb.transforms.PositiveInt

import scalapb.transforms.order.order2.order3.order3.TestMessage

class OrderSpec extends munit.FunSuite with ValidationHelpers {
  val m = TestMessage(bam = Some(PositiveInt(17)))
}
