package scalapb.validate

import examplepb.options.NoValidator

class OptionsSpec extends munit.FunSuite {

  test("Person empty") {
    assertNoDiff(
      compileErrors("Validator[NoValidator]"),
      """|error: no implicit argument of type scalapb.validate.Validator[examplepb.options.NoValidator] was found for an implicit parameter of method apply in object Validator
         |Validator[NoValidator]
         |                     ^""".stripMargin
    )
  }
}
