package scalapb.validate

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
          "No given instance of type scalapb.validate.Validator[third_party.third_party"
        ),
      clues(error)
    )
  }
}
