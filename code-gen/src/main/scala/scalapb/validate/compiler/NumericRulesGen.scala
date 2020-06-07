package scalapb.validate.compiler

import scala.language.reflectiveCalls
import Rule._
import scala.reflect.ClassTag

object NumericRulesGen {

  type NumericRules[T] = {
    def lte: Option[T]
    def lt: Option[T]
    def gt: Option[T]
    def gte: Option[T]
    def const: Option[T]
  }

  // constant definition
  private val NV = "scalapb.validate.NumericValidator"

  def constRule(javaType: String, const: String) =
    basic(s"$NV.constant[$javaType]", const)

  def numericRules[T](scalaType: String, rules: NumericRules[T])(implicit
      ct: ClassTag[T]
  ): Seq[Rule] = {
    val suffix = if (scalaType == "scala.Float") "f" else ""
    Seq(
      rules.gt.map(v =>
        basic(s"$NV.greaterThan[$scalaType]", v.toString() + suffix)
      ),
      rules.gte.map(v =>
        basic(s"$NV.greaterThanOrEqual[$scalaType]", v.toString() + suffix)
      ),
      rules.lt.map(v =>
        basic(s"$NV.lessThan[$scalaType]", v.toString() + suffix)
      ),
      rules.lte.map(v =>
        basic(s"$NV.lessThanOrEqual[$scalaType]", v.toString() + suffix)
      ),
      rules.const.map(v => constRule(scalaType, v.toString() + suffix))
    ).flatten
  }
}
