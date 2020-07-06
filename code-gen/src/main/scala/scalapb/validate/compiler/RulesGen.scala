package scalapb.validate.compiler

import com.google.protobuf.Descriptors.FieldDescriptor
import io.envoyproxy.pgv.validate.validate.FieldRules
import io.envoyproxy.pgv.validate.validate.FieldRules.Type
import scalapb.compiler.MethodApplication
import scalapb.validate.compiler.ComparativeRulesGen.{
  durationOrdering,
  timestampOrdering
}

object RulesGen {
  def rulesSingle(
      fd: FieldDescriptor,
      rules: FieldRules
  ): Seq[Rule] =
    rules.`type` match {
      case Type.String(stringRules) =>
        StringRulesGen.stringRules(fd, stringRules)

      case Type.Bytes(bytesRules) => BytesRuleGen.bytesRules(fd, bytesRules)

      case Type.Uint64(numericRules) =>
        ComparativeRulesGen.numericRules(numericRules)

      case Type.Sint64(numericRules) =>
        ComparativeRulesGen.numericRules(numericRules)

      case Type.Sfixed64(numericRules) =>
        ComparativeRulesGen.numericRules(numericRules)

      case Type.Fixed64(numericRules) =>
        ComparativeRulesGen.numericRules(numericRules)

      case Type.Int64(numericRules) =>
        ComparativeRulesGen.numericRules(numericRules)

      case Type.Uint32(numericRules) =>
        ComparativeRulesGen.numericRules(numericRules)

      case Type.Sint32(numericRules) =>
        ComparativeRulesGen.numericRules(numericRules)

      case Type.Sfixed32(numericRules) =>
        ComparativeRulesGen.numericRules(numericRules)

      case Type.Fixed32(numericRules) =>
        ComparativeRulesGen.numericRules(numericRules)

      case Type.Int32(numericRules) =>
        ComparativeRulesGen.numericRules(numericRules)

      case Type.Double(numericRules) =>
        ComparativeRulesGen.numericRules(numericRules)

      case Type.Float(numericRules) =>
        ComparativeRulesGen.numericRules(numericRules)

      case Type.Timestamp(timestampRules) =>
        ComparativeRulesGen.comparativeRules(timestampRules) ++
          TimestampRulesGen.timestampRules(timestampRules)

      case Type.Duration(durationRules) =>
        ComparativeRulesGen.numericRules(durationRules)

      case Type.Bool(boolRules) =>
        BooleanRulesGen.booleanRules(boolRules)

      case Type.Repeated(repeatedRules) =>
        RepeatedRulesGen.repeatedRules(repeatedRules)

      case Type.Any(anyRules) =>
        MembershipRulesGen.membershipRules(
          anyRules,
          MethodApplication("typeUrl")
        )

      case Type.Map(mapRules) =>
        MapRulesGen.mapRules(mapRules)

      case Type.Enum(enumRules) =>
        EnumRulesGen.enumRules(enumRules)

      case _ => Seq.empty
    }

  def isRequired(rules: FieldRules): Boolean =
    rules.getMessage.getRequired || (rules.`type` match {
      case Type.Timestamp(v) => v.getRequired
      case Type.Duration(v)  => v.getRequired
      case Type.Any(v)       => v.getRequired
      case _                 => false
    })
}
