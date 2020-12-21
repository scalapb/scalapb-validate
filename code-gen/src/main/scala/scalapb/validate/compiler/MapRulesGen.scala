package scalapb.validate.compiler

import scalapb.compiler.DescriptorImplicits
import io.envoyproxy.pgv.validate.validate.MapRules
import com.google.protobuf.Descriptors.FieldDescriptor

object MapRulesGen {
  val RR = "scalapb.validate.MapValidation"

  def mapRules(
      field: FieldDescriptor,
      rules: MapRules,
      desriptorImplicits: DescriptorImplicits
  ): Seq[Rule] = {
    import desriptorImplicits._
    Seq(
      rules.minPairs.map(value =>
        Rule.basic(
          s"$RR.minPairs",
          Seq(value.toString),
          inputTransform = field.collection.size
        )
      ),
      rules.maxPairs.map(value =>
        Rule.basic(
          s"$RR.maxPairs",
          Seq(value.toString),
          inputTransform = field.collection.size
        )
      ),
      Rule.ifSet(rules.getNoSparse)(Rule.basic(s"$RR.notSparse"))
    ).flatten
  }
}
