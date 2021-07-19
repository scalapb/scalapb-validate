package scalapb.transforms

import scalapb.TypeMapper

final case class PositiveInt(n: Int) {
  assert(n > 0)
}

object PositiveInt {
  implicit val tm: TypeMapper[Int, PositiveInt] =
    TypeMapper[Int, PositiveInt](PositiveInt(_))(_.n)
  implicit val ordering: Ordering[PositiveInt] =
    Ordering.fromLessThan[PositiveInt]((x, y) => x.n < y.n)
}
