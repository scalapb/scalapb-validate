package scalapb.validate.cats

import cats.Foldable
import cats.data.NonEmptyVector
import scalapb.validate.ValidationException
import scalapb.CollectionAdapter

class NonEmptyVectorAdapter[T] extends CollectionAdapter[T, NonEmptyVector[T]] {
  override def foreach(coll: NonEmptyVector[T])(f: T => Unit): Unit =
    coll.iterator.foreach(f)

  override def empty: NonEmptyVector[T] =
    throw new ValidationException(
      "No empty instance available for cats.Data.NonEmptyVector"
    )

  override def newBuilder: Builder =
    Vector
      .newBuilder[T]
      .mapResult(list =>
        NonEmptyVector
          .fromVector(list)
          .toRight(
            new ValidationException("Could not build an empty NonEmptyVector")
          )
      )

  override def concat(
      first: NonEmptyVector[T],
      second: Iterable[T]
  ): NonEmptyVector[T] =
    first.appendVector(second.toVector)

  override def toIterator(value: NonEmptyVector[T]): Iterator[T] =
    value.iterator

  override def size(value: NonEmptyVector[T]): Int =
    Foldable[NonEmptyVector].size(value).toInt
}

object NonEmptyVectorAdapter {
  def apply[T](): NonEmptyVectorAdapter[T] = new NonEmptyVectorAdapter[T]
}
