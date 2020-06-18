package myexample

import myexample.proto3.{Person, PersonValidator}

object Main {
  def main(args: Array[String]): Unit = {
    assert(
      PersonValidator.validate(
        Person(age = 12, name = Some("John"))
      ).toFailure.get.violation.getMessage() == "age: 12 must be greater than or equal to 18 - Got 12"
    )

    assert(
      PersonValidator.validate(
        Person(age = 20, name = None)
      ).toFailure.get.violation.getMessage() == "name: is required - Got None"
    )

    assert(
      PersonValidator.validate(
        Person(age = 20, name = Some("John"))
      ).isSuccess
    )
  }
}
