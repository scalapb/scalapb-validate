package scalapb.validate.compiler

import io.envoyproxy.pgv.validate.validate.FieldRules
import io.envoyproxy.pgv.validate.validate.FieldRules.Type

import scalapb.validate.compiler.NumericRulesGen.timestampOrdering
import scalapb.validate.compiler.NumericRulesGen.durationOrdering

object RulesGen {
  private val SCALA_INT = "scala.Int"
  private val SCALA_LONG = "scala.Long"
  private val SCALA_FLOAT = "scala.Float"
  private val SCALA_DOUBLE = "scala.Double"
  private val TIMESTAMP = "com.google.protobuf.timestamp.Timestamp"
  private val DURATION = "com.google.protobuf.duration.Duration"
  // import NumericRulesGen.timestampOrdering

  def rulesSingle(
      rules: FieldRules
  ): Seq[Rule] =
    rules.`type` match {
      case Type.String(stringRules) =>
        StringRulesGen.print(stringRules)

      case Type.Uint64(numericRules) =>
        NumericRulesGen.numericRules(SCALA_LONG, numericRules)

      case Type.Sint64(numericRules) =>
        NumericRulesGen.numericRules(SCALA_LONG, numericRules)

      case Type.Sfixed64(numericRules) =>
        NumericRulesGen.numericRules(SCALA_LONG, numericRules)

      case Type.Fixed64(numericRules) =>
        NumericRulesGen.numericRules(SCALA_LONG, numericRules)

      case Type.Int64(numericRules) =>
        NumericRulesGen.numericRules(SCALA_LONG, numericRules)

      case Type.Uint32(numericRules) =>
        NumericRulesGen.numericRules(SCALA_INT, numericRules)

      case Type.Sint32(numericRules) =>
        NumericRulesGen.numericRules(SCALA_INT, numericRules)

      case Type.Sfixed32(numericRules) =>
        NumericRulesGen.numericRules(SCALA_INT, numericRules)

      case Type.Fixed32(numericRules) =>
        NumericRulesGen.numericRules(SCALA_INT, numericRules)

      case Type.Int32(numericRules) =>
        NumericRulesGen.numericRules(SCALA_INT, numericRules)

      case Type.Double(numericRules) =>
        NumericRulesGen.numericRules(SCALA_DOUBLE, numericRules)

      case Type.Float(numericRules) =>
        NumericRulesGen.numericRules(SCALA_FLOAT, numericRules)

      case Type.Timestamp(timestampRules) =>
        NumericRulesGen.comparativeRules(TIMESTAMP, timestampRules)

      case Type.Duration(durationRules) =>
        NumericRulesGen.comparativeRules(DURATION, durationRules)

      case Type.Bool(boolRules) =>
        BooleanRulesGen.booleanRules(boolRules)

      case Type.Repeated(repeatedRules) =>
        RepeatedRulesGen.repeatedRules(repeatedRules)

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
