package scalapb.validate

import examplepb2.required.{Person => Person2}

class SkipSpec extends munit.FunSuite with ValidationHelpers {
  test("third_party validators dont exist") {
    val error =
      compileErrors(
        "implicitly[scalapb.validate.Validator[third_party.third_party.CantChangeThisMessage]]"
      )

    assert(
      error.contains(
        "could not find implicit value for parameter e: scalapb.validate.Validator[third_party.third_party.CantChangeThisMessage]"
      ) ||
        error.contains(
          "no implicit argument of type scalapb.validate.Validator["
        ),
      clues(error)
    )
  }
}
