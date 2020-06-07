package scalapb.validate.compiler

import io.envoyproxy.pgv.validate.validate.BoolRules

object BooleanRulesGen {
  private val JAVA_BOOLEAN = "java.lang.Boolean"

  def booleanRules(
      rules: BoolRules
  ): Seq[Rule] =
    Seq(
      rules.const.map(v =>
        NumericRulesGen.constRule(JAVA_BOOLEAN, v.toString())
      )
    ).flatten
}
