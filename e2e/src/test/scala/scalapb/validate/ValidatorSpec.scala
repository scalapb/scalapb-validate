package scalapb.validate

import examplepb.example.Person

class ValidatorSpec extends munit.FunSuite {

  def assertFailure[T](r: Result, expected: List[(String, AnyRef)])(implicit
      loc: munit.Location
  ) =
    r match {
      case Success => fail("expected a Failure, but was a Success")
      case Failure(violations) =>
        val fieldAndValues = violations.map { v =>
          v.getField -> v.getValue
        }
        assertEquals(fieldAndValues, expected)
    }

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
}
