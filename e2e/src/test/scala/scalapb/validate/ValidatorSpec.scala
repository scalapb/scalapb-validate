package scalapb.validate

import examplepb.example.Person
import examplepb2.required.{Person => Person2}
import examplepb3.optional.{Person => Person3}

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

  test("Optional field presence is recognized and validated") {
    assertFailure(
      Validator[Person3]
        .validate(
          Person3(name = None, age = Some(1), height = None)
        ),
      ("Person.name", "None") :: Nil
    )
  }
}
