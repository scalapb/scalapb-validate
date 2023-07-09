package scalapb.validate.cats

import cats.Foldable
import cats.data.Chain
import cats.data.NonEmptyChain
import scalapb.validate.ValidationException
import scalapb.CollectionAdapter

class NonEmptyChainAdapter[T] extends CollectionAdapter[T, NonEmptyChain[T]] {
  override def foreach(coll: NonEmptyChain[T])(f: T => Unit): Unit =
    coll.iterator.foreach(f)

  override def empty: NonEmptyChain[T] =
    throw new ValidationException(
      "No empty instance available for cats.Data.NonEmptyChain"
    )

  override def newBuilder: Builder =
    List
      .newBuilder[T]
      .mapResult(list =>
        NonEmptyChain
          .fromSeq(list)
          .toRight(
            new ValidationException("Could not build an empty NonEmptyChain")
          )
      )

  override def concat(
      first: NonEmptyChain[T],
      second: Iterable[T]
  ): NonEmptyChain[T] =
    first :++ Chain(second.toList: _*)

  override def toIterator(value: NonEmptyChain[T]): Iterator[T] = value.iterator

  override def size(value: NonEmptyChain[T]): Int =
    Foldable[NonEmptyChain].size(value).toInt
}

object NonEmptyChainAdapter {
  def apply[T](): NonEmptyChainAdapter[T] = new NonEmptyChainAdapter[T]
}
