package scalapb.validate.compiler

import io.envoyproxy.pgv.validate.validate.RepeatedRules

object RepeatedRulesGen {
  val RR = "scalapb.validate.RepeatedValidation"

  def repeatedRules(rules: RepeatedRules): Seq[Rule] =
    Seq(
      rules.minItems.map(value => Rule.basic(s"$RR.minItems", value.toString)),
      rules.maxItems.map(value => Rule.basic(s"$RR.maxItems", value.toString)),
      Rule.ifSet(rules.getUnique)(Rule.basic(s"$RR.unique"))
    ).flatten
}
