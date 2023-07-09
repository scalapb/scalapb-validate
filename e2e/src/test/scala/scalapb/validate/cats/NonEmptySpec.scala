package scalapb.validate.cats

import _root_.cats.data.NonEmptyChain
import _root_.cats.data.NonEmptySeq
import _root_.cats.data.NonEmptyVector
import e2e.cats.non_empty_seq.NonEmptySeqTest
import e2e.cats.non_empty_vector.NonEmptyVectorTest
import e2e.cats.non_empty_chain.NonEmptyChainTest
import scalapb.validate.ValidationHelpers
import scalapb.validate.Validator

class NonEmptySpec extends munit.FunSuite with ValidationHelpers {
  test("NonEmptySeq serialize and parse successfully") {
    val x = NonEmptySeqTest.of(NonEmptySeq.of(1, 2, 3))
    assertEquals(NonEmptySeqTest.parseFrom(x.toByteArray), x)
    assertEquals(NonEmptySeqTest.fromAscii(x.toProtoString), x)
    assert(Validator[NonEmptySeqTest].validate(x).isSuccess)
  }

  test("NonEmptyVector serialize and parse successfully") {
    val x = NonEmptyVectorTest.of(NonEmptyVector.of("a", "b", "c"))
    assertEquals(NonEmptyVectorTest.parseFrom(x.toByteArray), x)
    assertEquals(NonEmptyVectorTest.fromAscii(x.toProtoString), x)
    assert(Validator[NonEmptyVectorTest].validate(x).isSuccess)
  }

  test("NonEmptyChain serialize and parse successfully") {
    val x = NonEmptyChainTest.of(NonEmptyChain.of(true, false))
    assertEquals(NonEmptyChainTest.parseFrom(x.toByteArray), x)
    assertEquals(NonEmptyChainTest.fromAscii(x.toProtoString), x)
    assert(Validator[NonEmptyChainTest].validate(x).isSuccess)
  }
}
