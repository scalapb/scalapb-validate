package scalapb.validate

import examplepb2.required.{Person => Person2}

class SkipSpec extends munit.FunSuite with ValidationHelpers {
  test("third_party validators dont exist") {
    assertNoDiff(
      compileErrors(
        "implicitly[scalapb.validate.Validator[third_party.third_party.CantChangeThisMessage]]"
      ),
      """|error: no implicit argument of type scalapb.validate.Validator[third_party.third_party.CantChangeThisMessage] was found for parameter e of method implicitly in object Predef
         |implicitly[scalapb.validate.Validator[third_party.third_party.CantChangeThisMessage]]
         |                                                                                    ^
         |""".stripMargin
    )
  }
}
