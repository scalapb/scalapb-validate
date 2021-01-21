package scalapb.validate

import scalapb.CollectionAdapter
import collection.immutable.Set

class SetAdapter[T] extends CollectionAdapter[T, Set[T]] {
  def foreach(coll: Set[T])(f: T => Unit) = { coll.map(f); {} }

  def empty: Set[T] = Set.empty[T]

  def newBuilder: Builder =
    Vector
      .newBuilder[T]
      .mapResult(vec =>
        if (vec.distinct.size != vec.size)
          Left(new ValidationException("Got duplicate elements for Set"))
        else Right(vec.toSet)
      )

  def concat(first: Set[T], second: Iterable[T]) =
    throw new ValidationException(
      "No empty instance available for cats.Data.List"
    )

  def toIterator(value: Set[T]): Iterator[T] = value.iterator

  def size(value: Set[T]): Int = value.size
}

object SetAdapter {
  def apply[T]() = new SetAdapter[T]
}
