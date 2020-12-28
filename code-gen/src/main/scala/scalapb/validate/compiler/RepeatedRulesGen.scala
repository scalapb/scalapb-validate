package scalapb.validate.compiler

import io.envoyproxy.pgv.validate.validate.RepeatedRules
import scalapb.compiler.DescriptorImplicits
import com.google.protobuf.Descriptors.FieldDescriptor
import scalapb.compiler.MethodApplication

object RepeatedRulesGen {
  val RR = "scalapb.validate.RepeatedValidation"

  def repeatedRules(
      field: FieldDescriptor,
      rules: RepeatedRules,
      desriptorImplicits: DescriptorImplicits
  ): Seq[Rule] = {
    import desriptorImplicits._
    Seq(
      rules.minItems.map(value =>
        Rule.basic(
          s"$RR.minItems",
          Seq(value.toString),
          inputTransform = field.collection.size
        )
      ),
      rules.maxItems.map(value =>
        Rule.basic(
          s"$RR.maxItems",
          Seq(value.toString),
          inputTransform = field.collection.size
        )
      ),
      Rule.ifSet(
        rules.getUnique /* && !field
          .fieldOptions
          .getExtension(scalapb.validate.Validate.field)
          .getSkipUniqueCheck() */
      )(
        Rule.basic(
          s"$RR.unique",
          Seq(),
          inputTransform =
            field.collection.iterator.andThen(MethodApplication("toSeq"))
        )
      )
    ).flatten
  }
}
