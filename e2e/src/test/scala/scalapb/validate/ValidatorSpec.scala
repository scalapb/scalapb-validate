package scalapb.validate

import examplepb.example.Person
import examplepb2.required.{Person => Person2}

class ValidatorSpec extends munit.FunSuite with ValidationHelpers {
  val testPerson = Person(
    id = 1000,
    email = "foo@bar.com",
    name = "Protocol Buffer",
    home = Some(Person.Location(lat = 0, lng = 0)),
    age = 35
  )
  test("Person empty") {
    assertEquals(
      Validator[Person].validate(testPerson),
      Success
    )
  }
  test("Person invalid email") {
    assertFailure(
      Validator[Person].validate(testPerson.copy(email = "not an email")),
      ("Person.email", "\"not an email\"") :: Nil
    )
  }
  test("Person invalid email and age") {
    assertFailure(
      Validator[Person]
        .validate(testPerson.copy(email = "not an email", age = -1)),
      ("Person.email", "\"not an email\"") :: ("Person.age", Int.box(-1)) :: Nil
    )
  }
  test("Person invalid location lat") {
    assertFailure(
      Validator[Person]
        .validate(
          testPerson.copy(home = Some(Person.Location(lat = 100, lng = 0)))
        ),
      ("Person.Location.lat", Double.box(100)) :: Nil
    )
  }

  // See issue #21
  test("Required message is validated") {
    assertFailure(
      Validator[Person2]
        .validate(
          Person2(email = "foo@foo.com", home = Person2.Location(91, 0))
        ),
      ("Person.Location.lat", Double.box(91.0)) :: Nil
    )

  }

  test("assertValid does nothing if the object is valid") {
    Validator.assertValid(testPerson)(Validator[Person])
  }

  test("assertValid raises an Exception if the object is invalid") {
    interceptMessage[ValidationException](
      "Validation failed: Person.email: should be a valid email - Got \"not an email\", Person.age: -1 must be in the range [30, 40) - Got -1"
    ) {
      Validator.assertValid(testPerson.copy(email = "not an email", age = -1))(
        Validator[Person]
      )
    }
  }
}
