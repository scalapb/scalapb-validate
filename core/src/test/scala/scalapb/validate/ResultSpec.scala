package scalapb.validate

import io.envoyproxy.pgv.ValidationException

class ResultSpec extends munit.FunSuite {

  test("Success is success") {
    assertEquals(Success.isSuccess, true)
    assertEquals(Success.isFailure, false)
    assertEquals(Success.toFailure, None)
  }

  test("Failure is failure") {
    val failure = Failure(new ValidationException("field", 42, "reason"))
    assertEquals(failure.isSuccess, false)
    assertEquals(failure.isFailure, true)
    assertEquals(failure.toFailure, Some(failure))
  }

  test("Result && Result") {
    val failure = Failure(new ValidationException("field", 42, "reason"))
    val otherFailure = Failure(new ValidationException("field2", 24, "reason"))
    assertEquals(Success && Success, Success)
    assertEquals(Success && failure, failure)
    assertEquals(failure && Success, failure)
    assertEquals(
      failure && otherFailure,
      Failure(failure.violations ::: otherFailure.violations)
    )
    assertEquals(
      otherFailure && failure,
      Failure(otherFailure.violations ::: failure.violations)
    )
  }

  test("Result.apply") {
    val exception = new ValidationException("field", 42, "reason")
    assertEquals(
      Result(true, sys.error("failure branch should be lazy")),
      Success
    )
    assertEquals(Result(false, exception), Failure(exception))
  }

  test("Result.optional") {
    val exception = new ValidationException("field", 42, "reason")
    assertEquals(
      Result.optional(Some("value")) { value =>
        assertEquals(value, "value")
        Success
      },
      Success
    )
    assertEquals(
      Result.optional(Some("value")) { value =>
        assertEquals(value, "value")
        Failure(exception)
      },
      Failure(exception)
    )
    assertEquals(
      Result.optional[String](None) { _ =>
        sys.error("failure branch should be lazy")
      },
      Success
    )
  }

  test("Result.collect") {
    val failure = Failure(new ValidationException("field", 42, "reason"))
    val otherFailure = Failure(new ValidationException("field2", 24, "reason"))
    assertEquals(Result.collect(Nil), Success)
    assertEquals(Result.collect(Success :: Nil), Success)
    assertEquals(Result.collect(Success :: Success :: Nil), Success)
    assertEquals(Result.collect(Success :: failure :: Nil), failure)
    assertEquals(
      Result.collect(Success :: failure :: otherFailure :: Nil),
      failure && otherFailure
    )
    assertEquals(
      Result.collect(failure :: otherFailure :: Success :: Nil),
      failure && otherFailure
    )
    assertEquals(
      Result.collect(otherFailure :: failure :: Success :: Nil),
      otherFailure && failure
    )
  }

  test("Result.repeated") {
    val failure = Failure(new ValidationException("field", 42, "reason"))
    val otherFailure = Failure(new ValidationException("field2", 24, "reason"))
    assertEquals(
      Result.repeated(Nil)(_ => sys.error("cannot be invoked")),
      Success
    )
    assertEquals(Result.repeated(Success :: Nil)(identity), Success)
    assertEquals(Result.repeated(Success :: Success :: Nil)(identity), Success)
    assertEquals(Result.repeated(Success :: failure :: Nil)(identity), failure)
    assertEquals(
      Result.repeated(Success :: failure :: otherFailure :: Nil)(identity),
      failure && otherFailure
    )
    assertEquals(
      Result.repeated(failure :: otherFailure :: Success :: Nil)(identity),
      failure && otherFailure
    )
    assertEquals(
      Result.repeated(otherFailure :: failure :: Success :: Nil)(identity),
      otherFailure && failure
    )
  }
}
