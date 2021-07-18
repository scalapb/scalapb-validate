package scalapb.validate.compiler

import io.envoyproxy.pgv.validate.validate.RepeatedRules
import scalapb.compiler.DescriptorImplicits
import com.google.protobuf.Descriptors.FieldDescriptor
import scalapb.compiler.MethodApplication

object RepeatedRulesGen {
  val RR = "scalapb.validate.RepeatedValidation"

  def maxItems(
      fd: FieldDescriptor,
      value: Long,
      desriptorImplicits: DescriptorImplicits
  ): FunctionCall = {
    import desriptorImplicits._
    Rule.basic(
      s"$RR.maxItems",
      Seq(value.toString),
      inputTransform = fd.collection.size
    )
  }

  def minItems(
      fd: FieldDescriptor,
      value: Long,
      desriptorImplicits: DescriptorImplicits
  ): FunctionCall = {
    import desriptorImplicits._
    Rule.basic(
      s"$RR.minItems",
      Seq(value.toString),
      inputTransform = fd.collection.size
    )
  }

  def repeatedRules(
      field: FieldDescriptor,
      rules: RepeatedRules,
      desriptorImplicits: DescriptorImplicits
  ): Seq[Rule] = {
    import desriptorImplicits._
    Seq(
      rules.minItems.map(value => minItems(field, value, desriptorImplicits)),
      rules.maxItems.map(value => maxItems(field, value, desriptorImplicits)),
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
