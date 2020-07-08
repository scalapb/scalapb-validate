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
      ("email", "\"not an email\"") :: Nil
    )
  }
  test("Person invalid email and age") {
    assertFailure(
      Validator[Person]
        .validate(testPerson.copy(email = "not an email", age = -1)),
      ("email", "\"not an email\"") :: ("age", Int.box(-1)) :: Nil
    )
  }
}
