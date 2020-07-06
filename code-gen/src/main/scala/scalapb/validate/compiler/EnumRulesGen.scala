package scalapb.validate.compiler

import io.envoyproxy.pgv.validate.validate.EnumRules
import scalapb.compiler.MethodApplication
import Rule.ifSet

object EnumRulesGen {
  private[validate] val EV = "scalapb.validate.EnumValidation"
  private[validate] val CV = "scalapb.validate.ComparativeValidation"

  def enumRules(
      rules: EnumRules
  ): Seq[Rule] =
    MembershipRulesGen.membershipRules(
      rules,
      transform = MethodApplication("value")
    ) ++
      Seq(
        ifSet(rules.getDefinedOnly)(Rule.basic(s"$EV.definedOnly")),
        rules.const.map(c =>
          Rule.basic(
            s"$CV.constant",
            Seq(c.toString),
            inputTransform = MethodApplication("value")
          )
        )
      ).flatten
}
