package scalapb.validate.compiler

import scala.language.reflectiveCalls
import scala.math.Ordering.Implicits._
import com.google.protobuf.timestamp.Timestamp
import Rule.basic
import com.google.protobuf.duration.Duration

object NumericRulesGen {

  type ComparativeRules[T] = {
    def lte: Option[T]
    def lt: Option[T]
    def gt: Option[T]
    def gte: Option[T]
    def const: Option[T]
  }

  type MembershipRules[T] = {
    def in: Seq[T]
    def notIn: Seq[T]
  }

  type NumericRules[T] = ComparativeRules[T] with MembershipRules[T]

  def numericRules[T: Numeric](
      scalaType: String,
      rules: NumericRules[T]
  ): Seq[Rule] =
    comparativeRules(scalaType, rules) ++ membershipRules(scalaType, rules)

  // constant definition
  private val NV = "scalapb.validate.NumericValidator"

  def constRule(scalaType: String, const: String) =
    basic(s"$NV.constant[$scalaType]", const)

  def show[T](scalaType: String, v: T) =
    if (scalaType == "scala.Float") s"${v}f"
    else if (scalaType == "scala.Long") s"${v}L"
    else if (scalaType == "com.google.protobuf.timestamp.Timestamp")
      s"com.google.protobuf.timestamp.Timestamp.of(${v.asInstanceOf[Timestamp].seconds}, ${v.asInstanceOf[Timestamp].nanos})"
    else if (scalaType == "com.google.protobuf.duration.Duration")
      s"com.google.protobuf.duration.Duration.of(${v.asInstanceOf[Duration].seconds}, ${v.asInstanceOf[Duration].nanos})"
    else v.toString()

  def comparativeRules[T: Ordering](
      scalaType: String,
      rules: ComparativeRules[T]
  ): Seq[Rule] = {
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
      rules.const.map(v => constRule(scalaType, show(scalaType, v)))
    ).flatten

    val rangeRules = (maybeGtVal, maybeLtVal) match {
      case (Some(gtVal), Some(ltVal)) =>
        val ex = if (ltVal < gtVal) "Ex" else ""
        Seq(
          basic(
            s"$NV.range$gtType$ltType$ex[$scalaType]",
            show(scalaType, gtVal),
            show(scalaType, ltVal)
          )
        )
      case _ =>
        Seq(
          rules.gt.map(v =>
            basic(s"$NV.greaterThan[$scalaType]", show(scalaType, v))
          ),
          rules.gte.map(v =>
            basic(s"$NV.greaterThanOrEqual[$scalaType]", show(scalaType, v))
          ),
          rules.lt
            .map(v => basic(s"$NV.lessThan[$scalaType]", show(scalaType, v))),
          rules.lte.map(v =>
            basic(s"$NV.lessThanOrEqual[$scalaType]", show(scalaType, v))
          )
        ).flatten
    }

    rangeRules ++ constRules
  }

  def membershipRules[T](scalaType: String, rules: MembershipRules[T]) =
    Seq(
      if (rules.in.nonEmpty)
        Some(
          basic(
            s"$NV.in[$scalaType]",
            rules.in.map(v => show(scalaType, v)).mkString("Seq(", ", ", ")")
          )
        )
      else None,
      if (rules.notIn.nonEmpty)
        Some(
          basic(
            s"$NV.notIn[$scalaType]",
            rules.notIn.map(v => show(scalaType, v)).mkString("Seq(", ", ", ")")
          )
        )
      else None
    ).flatten

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
