package scalapb.validate.compiler

import scalapb.compiler.{Expression, Identity}
import scalapb.validate.compiler.Rule._

import scala.language.reflectiveCalls
import scala.reflect.{classTag, ClassTag}

object MembershipRulesGen {
  private val MV: String = "scalapb.validate.MembershipValidation"

  type MembershipRules[T] = {
    def in: Seq[T]
    def notIn: Seq[T]
  }

  def membershipRules[T: ClassTag](
      rules: MembershipRules[T],
      transform: Expression = Identity
  )(implicit show: Show[T]) = {
    val runtimeClass = classTag[T].runtimeClass
    val className =
      if (runtimeClass.isPrimitive()) runtimeClass.getName() match {
        case "int"    => "Int"
        case "long"   => "Long"
        case "float"  => "Float"
        case "double" => "Double"
      }
      else runtimeClass.getName()

    Seq(
      if (rules.in.nonEmpty)
        Some(
          basic(
            s"$MV.in[$className]",
            Seq(rules.in.map(v => show(v)).mkString("Seq(", ", ", ")")),
            transform
          )
        )
      else None,
      if (rules.notIn.nonEmpty)
        Some(
          basic(
            s"$MV.notIn[$className]",
            Seq(rules.notIn.map(v => show(v)).mkString("Seq(", ", ", ")")),
            transform
          )
        )
      else None
    ).flatten
  }

}
