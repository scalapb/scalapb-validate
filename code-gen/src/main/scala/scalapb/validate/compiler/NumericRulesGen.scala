package scalapb.validate.compiler

import scala.language.reflectiveCalls
import scala.math.Ordering.Implicits._
import Rule._

object NumericRulesGen {

  type NumericRules[T] = {
    def lte: Option[T]
    def lt: Option[T]
    def gt: Option[T]
    def gte: Option[T]
    def const: Option[T]
    def in: Seq[T]
    def notIn: Seq[T]
  }

  // constant definition
  private val NV = "scalapb.validate.NumericValidator"

  def constRule(scalaType: String, const: String) =
    basic(s"$NV.constant[$scalaType]", const)

  def numericRules[T: Numeric](
      scalaType: String,
      rules: NumericRules[T]
  ): Seq[Rule] = {
    val suffix =
      if (scalaType == "scala.Float") "f"
      else if (scalaType == "scala.Long") "L"
      else
        ""
    if (rules.gt.isDefined && rules.gte.isDefined)
      new RuntimeException("Error: both gt and gte were specified.")
    if (rules.lt.isDefined && rules.lte.isDefined)
      new RuntimeException("Error: both lt and lte were specified.")
    val gtType =
      if (rules.gt.isDefined) "Gt"
      else if (rules.gte.isDefined) "Gte"
      else ""
    val ltType =
      if (rules.lt.isDefined) "Lt"
      else if (rules.lte.isDefined) "Lte"
      else ""
    val zero = implicitly[Numeric[T]].zero
    val gtVal = rules.gt.getOrElse(rules.gte.getOrElse(zero))
    val ltVal = rules.lt.getOrElse(rules.lte.getOrElse(zero))

    val constRules = Seq(
      rules.const.map(v => constRule(scalaType, v.toString() + suffix))
    ).flatten

    val inRules = Seq(
      if (rules.in.nonEmpty)
        Some(
          basic(
            s"$NV.in[$scalaType]",
            rules.in.map(_.toString() + suffix).mkString("Seq(", ", ", ")")
          )
        )
      else None,
      if (rules.notIn.nonEmpty)
        Some(
          basic(
            s"$NV.notIn[$scalaType]",
            rules.notIn.map(_.toString() + suffix).mkString("Seq(", ", ", ")")
          )
        )
      else None
    ).flatten

    val rangeRules = if ((gtType != "") && (ltType != "")) {
      val ex = if (ltVal < gtVal) "Ex" else ""
      Seq(
        basic(
          s"$NV.range$gtType$ltType$ex[$scalaType]",
          gtVal.toString + suffix,
          ltVal.toString + suffix
        )
      )
    } else
      Seq(
        rules.gt.map(v =>
          basic(s"$NV.greaterThan[$scalaType]", v.toString() + suffix)
        ),
        rules.gte.map(v =>
          basic(s"$NV.greaterThanOrEqual[$scalaType]", v.toString() + suffix)
        ),
        rules.lt
          .map(v => basic(s"$NV.lessThan[$scalaType]", v.toString() + suffix)),
        rules.lte.map(v =>
          basic(s"$NV.lessThanOrEqual[$scalaType]", v.toString() + suffix)
        )
      ).flatten

    rangeRules ++ constRules ++ inRules
  }
}
