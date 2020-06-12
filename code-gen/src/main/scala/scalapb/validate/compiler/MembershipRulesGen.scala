package scalapb.validate.compiler

import scala.reflect.ClassTag
import scala.reflect.classTag
import scala.language.reflectiveCalls
import scalapb.compiler.Identity
import scalapb.compiler.Expression
import Rule._

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
