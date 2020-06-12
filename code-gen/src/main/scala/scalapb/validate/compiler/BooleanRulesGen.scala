package scalapb.validate.compiler

import io.envoyproxy.pgv.validate.validate.BoolRules

object BooleanRulesGen {
  def booleanRules(
      rules: BoolRules
  ): Seq[Rule] =
    Seq(
      rules.const.map(v =>
        ComparativeRulesGen.constRule(v.toString())
      )
    ).flatten
}
