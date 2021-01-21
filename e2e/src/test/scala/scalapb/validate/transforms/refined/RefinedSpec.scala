package scalapb.validate.transforms.refined

import scalapb.transforms.refined.refined.RefinedTest
import scalapb.validate.ValidationException
import eu.timepit.refined.auto._

class RefinedSpec extends munit.FunSuite {
  val m = RefinedTest(gt5 = 6, constant = 17, oc = 37.0)

  test("Valid message is parsable") {
    assertEquals(RefinedTest.parseFrom(m.toByteArray), m)
  }

  test("Not instantiable with default values") {
    interceptMessage[ValidationException]("Predicate failed: (0 > 5).") {
      RefinedTest()
    }
  }

  test("Refined types correctly installed") {
    assertNoDiff(
      compileErrors("m.withGt5(3)"),
      """|error: Predicate failed: (3 > 5).
         |m.withGt5(3)
         |          ^
         |""".stripMargin
    )

    assertNoDiff(
      compileErrors("m.withConstant(14)"),
      """|error: Predicate failed: (14 == 17).
         |m.withConstant(14)
         |               ^
         |""".stripMargin
    )

    assertNoDiff(
      compileErrors("m.withOc(0.0)"),
      """|error: Left predicate of ((0.0 > 0.0) && !(0.0 > 100.0)) failed: Predicate failed: (0.0 > 0.0).
         |m.withOc(0.0)
         |         ^
         |""".stripMargin
    )
  }

}
