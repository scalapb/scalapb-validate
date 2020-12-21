package scalapb.validate

trait ValidationHelpers {
  this: munit.FunSuite =>

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

}
