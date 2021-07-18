package scalapb.validate.compiler

import com.google.protobuf.Descriptors.FieldDescriptor
import io.envoyproxy.pgv.validate.validate.FieldRules
import io.envoyproxy.pgv.validate.validate.FieldRules.Type
import scalapb.compiler.DescriptorImplicits
import scala.language.reflectiveCalls

object IgnoreEmptyRulesGen {
  type HasIgnoreEmpty = {
    def getIgnoreEmpty: Boolean
  }

  def numericIsEmpty(hi: HasIgnoreEmpty): Option[Rule] =
    Rule.ifSet(hi.getIgnoreEmpty)(ComparativeRulesGen.constRule("0"))

  def ignoreEmptyRule(
      fd: FieldDescriptor,
      rules: FieldRules,
      di: DescriptorImplicits
  ): Option[Rule] =
    rules.`type` match {
      case Type.Repeated(rules) =>
        Rule.ifSet(rules.getIgnoreEmpty)(RepeatedRulesGen.maxItems(fd, 0, di))

      case Type.String(rules) =>
        Rule.ifSet(rules.getIgnoreEmpty)(StringRulesGen.maxLen(0))

      case Type.Bytes(rules) =>
        Rule.ifSet(rules.getIgnoreEmpty)(BytesRuleGen.maxLength(0))

      case Type.Uint64(numericRules) =>
        numericIsEmpty(numericRules)

      case Type.Sint64(numericRules) =>
        numericIsEmpty(numericRules)

      case Type.Sfixed64(numericRules) =>
        numericIsEmpty(numericRules)

      case Type.Fixed64(numericRules) =>
        numericIsEmpty(numericRules)

      case Type.Int64(numericRules) =>
        numericIsEmpty(numericRules)

      case Type.Uint32(numericRules) =>
        numericIsEmpty(numericRules)

      case Type.Sint32(numericRules) =>
        numericIsEmpty(numericRules)

      case Type.Sfixed32(numericRules) =>
        numericIsEmpty(numericRules)

      case Type.Fixed32(numericRules) =>
        numericIsEmpty(numericRules)

      case Type.Int32(numericRules) =>
        numericIsEmpty(numericRules)

      case Type.Double(numericRules) =>
        numericIsEmpty(numericRules)

      case Type.Float(numericRules) =>
        numericIsEmpty(numericRules)

      case Type.Timestamp(_) =>
        None

      case Type.Duration(_) =>
        None

      case Type.Bool(_) =>
        None

      case Type.Any(anyRules) =>
        None

      case Type.Map(mapRules) =>
        Rule.ifSet(mapRules.getIgnoreEmpty)(
          RepeatedRulesGen.maxItems(fd, 0, di)
        )

      case Type.Enum(enumRules) =>
        None

      case _ => None
    }

}
