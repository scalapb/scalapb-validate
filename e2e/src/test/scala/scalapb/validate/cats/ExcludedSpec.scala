package scalapb.validate.cats

import e2e.cats.excluded.Excluded

class ExcludedSpec extends munit.FunSuite {
  // We are testing that nonEmptySet has not been transformed into
  // cats.data.NonEmptySet since the proto file is excluded.
  val m = Excluded(nonEmptySet = List())
}
