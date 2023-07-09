package scalapb.validate.cats

import cats.Foldable
import cats.data.NonEmptySeq
import scalapb.validate.ValidationException
import scalapb.CollectionAdapter

class NonEmptySeqAdapter[T] extends CollectionAdapter[T, NonEmptySeq[T]] {
  override def foreach(coll: NonEmptySeq[T])(f: T => Unit): Unit =
    coll.iterator.foreach(f)

  override def empty: NonEmptySeq[T] =
    throw new ValidationException(
      "No empty instance available for cats.Data.NonEmptySeq"
    )

  override def newBuilder: Builder =
    List
      .newBuilder[T]
      .mapResult(list =>
        NonEmptySeq
          .fromSeq(list)
          .toRight(
            new ValidationException("Could not build an empty NonEmptySeq")
          )
      )

  override def concat(
      first: NonEmptySeq[T],
      second: Iterable[T]
  ): NonEmptySeq[T] =
    first.appendSeq(second.toList)

  override def toIterator(value: NonEmptySeq[T]): Iterator[T] = value.iterator

  override def size(value: NonEmptySeq[T]): Int =
    Foldable[NonEmptySeq].size(value).toInt
}

object NonEmptySeqAdapter {
  def apply[T](): NonEmptySeqAdapter[T] = new NonEmptySeqAdapter[T]
}
