package scalapb.validate

import examplepb2.required.{Person => Person2}

class SkipSpec extends munit.FunSuite with ValidationHelpers {
  test("third_party validators dont exist") {
    assertNoDiff(
      compileErrors(
        "implicitly[scalapb.validate.Validator[third_party.third_party.CantChangeThisMessage]]"
      ),
      """|error: could not find implicit value for parameter e: scalapb.validate.Validator[third_party.third_party.CantChangeThisMessage]
      |implicitly[scalapb.validate.Validator[third_party.third_party.CantChangeThisMessage]]
      |          ^
      |""".stripMargin
    )
  }
}
