package scalapb.validate.compiler

import scala.language.reflectiveCalls
import scala.math.Ordering.Implicits._
import com.google.protobuf.timestamp.Timestamp
import Rule.basic
import com.google.protobuf.duration.Duration
import scala.reflect.ClassTag
import scala.reflect.classTag

object ComparativeRulesGen {
  val TimestampOrdering = "scalapb.validate.ComparativeValidation.timestampOrdering"
  val DurationOrdering = "scalapb.validate.ComparativeValidation.durationOrdering"

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
      rules: NumericRules[T]
  ): Seq[Rule] =
    comparativeRules(rules) ++ MembershipRulesGen.membershipRules[T](
      rules
    )

  def additionalImports(className: String): Seq[String] =
    className match {
      case "com.google.protobuf.timestamp.Timestamp" => Seq(TimestampOrdering)
      case "com.google.protobuf.duration.Duration"   => Seq(DurationOrdering)
      case _                                         => Nil
    }

  // constant definition
  private[validate] val CV = "scalapb.validate.ComparativeValidation"

  def constRule(const: String) =
    basic(s"$CV.constant", const)

  def comparativeRules[T: Ordering: ClassTag](
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
      rules.const.map(v => constRule(show(v)))
    ).flatten

    val rangeRules = (maybeGtVal, maybeLtVal) match {
      case (Some(gtVal), Some(ltVal)) =>
        val ex = if (ltVal < gtVal) "Ex" else ""
        Seq(
          basic(
            s"$CV.range$gtType$ltType$ex",
            Seq(
              show(gtVal),
              show(ltVal)
            )
          )
        )
      case _ =>
        Seq(
          rules.gt.map(v => basic(s"$CV.greaterThan", show(v))),
          rules.gte.map(v =>
            basic(s"$CV.greaterThanOrEqual", show(v))
          ),
          rules.lt
            .map(v => basic(s"$CV.lessThan", show(v))),
          rules.lte.map(v => basic(s"$CV.lessThanOrEqual", show(v)))
        ).flatten
    }

    val imports = additionalImports(classTag[T].runtimeClass.getName)
    (rangeRules ++ constRules).map(rule =>
      imports.foldLeft[Rule](rule)(_.withImport(_))
    )
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
