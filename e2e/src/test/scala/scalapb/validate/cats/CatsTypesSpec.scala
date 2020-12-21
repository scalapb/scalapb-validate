package scalapb.validate.cats

import cats.data.{NonEmptyList, NonEmptyMap, NonEmptySet}
import e2e.cats.types.{NonEmptyTypes, NonEmptyTypesWithSubRules}
import com.google.protobuf.InvalidProtocolBufferException
import scalapb.validate.Success
import scalapb.validate.Validator
import scalapb.validate.ValidationHelpers

class CatsTypesSpec extends munit.FunSuite with ValidationHelpers {
  test("NonEmptyTypes serialize and parse successfully") {
    val p = NonEmptyTypes(
      nonEmptySet = NonEmptySet.of("foo", "bar"),
      nonEmptyList = NonEmptyList.of("bar", "baz"),
      nonEmptyMap = NonEmptyMap.of(3 -> 4)
    )
    assertEquals(NonEmptyTypes.parseFrom(p.toByteArray), p)
    assertEquals(NonEmptyTypes.fromAscii(p.toProtoString), p)
    assertEquals(Validator[NonEmptyTypes].validate(p).isSuccess, true)
  }

  test("NonEmptyTypes fails to construct if invalid") {
    val p = NonEmptyTypes(
      nonEmptySet = NonEmptySet.of("foo", "bar"),
      nonEmptyList = NonEmptyList.of("bar", "baz"),
      nonEmptyMap = NonEmptyMap.of(3 -> 4)
    )
    intercept[IllegalArgumentException] {
      p.copy(foo = "verylongstring")
    }
  }

  test("NonEmptyTypes fail when input is empty") {
    interceptMessage[InvalidProtocolBufferException](
      "NonEmptySet must be non-empty"
    )(NonEmptyTypes.parseFrom(Array[Byte]()))
  }

  val subrules = NonEmptyTypesWithSubRules(
    nonEmptySet = NonEmptySet.of("fooba", "gooma"),
    nonEmptyList = NonEmptyList.of("fooba", "gooma"),
    nonEmptyMap = NonEmptyMap.of(4 -> 5)
  )

  test("NonEmptyTypesWithSubRules should validate correctly") {
    assertEquals(
      Validator[NonEmptyTypesWithSubRules].validate(subrules),
      Success
    )
  }

  test("NonEmptyTypesWithSubRules should fail member validation") {
    val invalid1 =
      subrules.update(
        _.nonEmptySet := NonEmptySet.of("foo")
      ) // str length is not 5
    val invalid2 =
      subrules.update(
        _.nonEmptyList := NonEmptyList.of("foo")
      ) // str length is not 5
    val invalid3 =
      subrules.update(_.nonEmptyMap := NonEmptyMap.of(1 -> 12)) // key not gte 4
    val invalid4 =
      subrules.update(
        _.nonEmptyMap := NonEmptyMap.of(4 -> 4)
      ) // value not gte 5

    assertFailure(
      Validator[NonEmptyTypesWithSubRules].validate(invalid1),
      List(
        ("NonEmptyTypesWithSubRules.non_empty_set", "\"foo\"")
      )
    )
    assertFailure(
      Validator[NonEmptyTypesWithSubRules].validate(invalid2),
      List(
        ("NonEmptyTypesWithSubRules.non_empty_list", "\"foo\"")
      )
    )
    assertFailure(
      Validator[NonEmptyTypesWithSubRules].validate(invalid3),
      List(
        ("NonEmptyTypesWithSubRules.NonEmptyMapEntry.key", Int.box(1))
      )
    )
    assertFailure(
      Validator[NonEmptyTypesWithSubRules].validate(invalid4),
      List(
        ("NonEmptyTypesWithSubRules.NonEmptyMapEntry.value", Int.box(4))
      )
    )

  }

}
