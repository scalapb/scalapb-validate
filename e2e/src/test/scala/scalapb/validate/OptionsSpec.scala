package scalapb.validate

import examplepb.options.NoValidator

class OptionsSpec extends munit.FunSuite {

  test("Person empty") {
    val error =
      compileErrors("Validator[NoValidator]")
    assert(
      error.contains(
        "could not find implicit value for evidence parameter of type scalapb.validate.Validator["
      ) ||
        error.contains(
          "No given instance of type scalapb.validate.Validator[examplepb.options.NoValidator] was found"
        ),
      clues(error)
    )
  }
}
