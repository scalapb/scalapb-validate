package scalapb.validate

import examplepb.options.NoValidator

class OptionsSpec extends munit.FunSuite {

  test("Person empty") {
    assertNoDiff(
      compileErrors("Validator[NoValidator]"),
      """|error: could not find implicit value for evidence parameter of type scalapb.validate.Validator[examplepb.options.NoValidator]
         |Validator[NoValidator]
         |         ^""".stripMargin
    )
  }
}
