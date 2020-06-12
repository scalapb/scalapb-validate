package scalapb.validate.compiler

import scala.language.reflectiveCalls
import scala.math.Ordering.Implicits._
import com.google.protobuf.timestamp.Timestamp
import Rule.basic
import com.google.protobuf.duration.Duration
import scala.reflect.ClassTag

object NumericRulesGen {

  type ComparativeRules[T] = {
    def lte: Option[T]
    def lt: Option[T]
    def gt: Option[T]
    def gte: Option[T]
    def const: Option[T]
  }

  type NumericRules[T] = ComparativeRules[T]
    with MembershipRulesGen.MembershipRules[T]

  def numericRules[T: Ordering: Show: ClassTag](
      scalaType: String,
      rules: NumericRules[T]
  ): Seq[Rule] =
    comparativeRules(scalaType, rules) ++ MembershipRulesGen.membershipRules[T](
      rules
    )

  // constant definition
  private[validate] val NV = "scalapb.validate.NumericValidation"

  def constRule(scalaType: String, const: String) =
    basic(s"$NV.constant[$scalaType]", const)

  def comparativeRules[T: Ordering](
      scalaType: String,
      rules: ComparativeRules[T]
  )(implicit show: Show[T]): Seq[Rule] = {
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
    val maybeGtVal = rules.gt.orElse(rules.gte)
    val maybeLtVal = rules.lt.orElse(rules.lte)

    val constRules = Seq(
      rules.const.map(v => constRule(scalaType, show(v)))
    ).flatten

    val rangeRules = (maybeGtVal, maybeLtVal) match {
      case (Some(gtVal), Some(ltVal)) =>
        val ex = if (ltVal < gtVal) "Ex" else ""
        Seq(
          basic(
            s"$NV.range$gtType$ltType$ex[$scalaType]",
            Seq(
              show(gtVal),
              show(ltVal)
            )
          )
        )
      case _ =>
        Seq(
          rules.gt.map(v => basic(s"$NV.greaterThan[$scalaType]", show(v))),
          rules.gte.map(v =>
            basic(s"$NV.greaterThanOrEqual[$scalaType]", show(v))
          ),
          rules.lt
            .map(v => basic(s"$NV.lessThan[$scalaType]", show(v))),
          rules.lte.map(v => basic(s"$NV.lessThanOrEqual[$scalaType]", show(v)))
        ).flatten
    }

    rangeRules ++ constRules
  }

  implicit val timestampOrdering = new Ordering[Timestamp] {
    def compare(x: Timestamp, y: Timestamp): Int = {
      val o1 = java.lang.Long.compare(x.seconds, y.seconds)
      if (o1 != 0) o1
      else java.lang.Integer.compare(x.nanos, y.nanos)
    }
  }

  implicit val durationOrdering = new Ordering[Duration] {
    def compare(x: Duration, y: Duration): Int = {
      val o1 = java.lang.Long.compare(x.seconds, y.seconds)
      if (o1 != 0) o1
      else java.lang.Integer.compare(x.nanos, y.nanos)
    }
  }
}
