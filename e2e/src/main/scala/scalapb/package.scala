package scalapb

import test.oneofs.TestBigDecimal

package object test {
  implicit val tm: TypeMapper[TestBigDecimal, BigDecimal] =
    TypeMapper[TestBigDecimal, BigDecimal](_ => BigDecimal(0.0))(_ =>
      TestBigDecimal()
    )
}
