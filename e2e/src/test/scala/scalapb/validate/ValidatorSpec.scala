package scalapb.validate

import examplepb.example.Person

class ValidatorSpec extends munit.FunSuite {
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
}
