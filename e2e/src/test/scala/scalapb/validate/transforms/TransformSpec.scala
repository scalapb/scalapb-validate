package scalapb.validate.transforms

import scalapb.validate.ValidationException
import scalapb.validate.ValidationHelpers
import scalapb.transforms.field.{MyTestMessage, MyTestMessageWithNonEmpty}
import scalapb.transforms.PositiveInt
import scalapb.validate.{Success, Validator}
import cats.data.NonEmptyList
import cats.data.NonEmptyMap
import cats.data.NonEmptySet
import scalapb.transforms.MyCustomType

class TransformsSpec extends munit.FunSuite with ValidationHelpers {
  val inst = MyTestMessage(optPosNum = PositiveInt(4))

  test("MyTestMessage default constructor fails") {
    interceptMessage[AssertionError]("assertion failed")(MyTestMessage())
  }

  val msg = MyTestMessage(
    optPosNum = PositiveInt(17),
    repPosNum = Seq(PositiveInt(3)),
    setPosNum = Set(PositiveInt(4)),
    Map(PositiveInt(4) -> PositiveInt(6))
  )

  test("MyTestMessage has correct types") {
    assertEquals(MyTestMessage.parseFrom(msg.toByteArray), msg)
    assertEquals(Validator[MyTestMessage].validate(msg), Success)
  }

  test("MyTestMessage will not instantiate invalid") {
    assert(intercept[ValidationException] {
      msg.copy(foo = -20)
    }.getMessage().contains("MyTestMessage.foo: -20 must be greater than -5"))
  }

  test("MyTestMessageWithNonEmpty has correct types") {
    implicit val t = cats.kernel.Order.fromOrdering(PositiveInt.ordering)
    val msg = MyTestMessageWithNonEmpty(
      optPosNum = PositiveInt(17),
      setPosNum = NonEmptySet.of(PositiveInt(9)),
      repPosNum = NonEmptyList.of(PositiveInt(3)),
      mapPosNums = NonEmptyMap.of(PositiveInt(4) -> PositiveInt(6)),
      mapMsg = NonEmptyMap.of(PositiveInt(4) -> MyCustomType("boo"))
    )
    assertEquals(MyTestMessageWithNonEmpty.parseFrom(msg.toByteArray), msg)
    assertEquals(Validator[MyTestMessageWithNonEmpty].validate(msg), Success)
    intercept[ValidationException] {
      msg.withMapMsg(
        NonEmptyMap.of(PositiveInt(4) -> MyCustomType("boom")) // must be boo
      )
    }
  }
}
