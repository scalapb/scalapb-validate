package scalapb.validate.compiler

import io.envoyproxy.pgv.validate.validate.MapRules

object MapRulesGen {
  val RR = "scalapb.validate.MapValidation"
  def mapRules(rules: MapRules) =
    Seq(
      rules.minPairs.map(value => Rule.basic(s"$RR.minPairs", value.toString)),
      rules.maxPairs.map(value => Rule.basic(s"$RR.maxPairs", value.toString)),
      Rule.ifSet(rules.getNoSparse)(Rule.basic(s"$RR.notSparse"))
      // Rule.ifSet(rules.getUnique)(Rule.basic(s"$RR.unique"))
    ).flatten

}
