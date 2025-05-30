package scalapb.validate.compiler

import com.google.protobuf.Descriptors.FieldDescriptor
import io.envoyproxy.pgv.validate.validate.FieldRules
import io.envoyproxy.pgv.validate.validate.FieldRules.Type
import scalapb.compiler.DescriptorImplicits
import scala.language.reflectiveCalls
import ComparativeRulesGen.NumericRules
import scala.reflect.{classTag, ClassTag}

object IgnoreEmptyRulesGen {
  type HasIgnoreEmpty = {
    def getIgnoreEmpty: Boolean
  }

  def numericIsEmpty[T: ClassTag](
      rules: NumericRules[T] with HasIgnoreEmpty
  ): Option[Rule] = {
    val zero = classTag[T] match {
      case ct if ct == classTag[Long]   => "0L"
      case ct if ct == classTag[Int]    => "0"
      case ct if ct == classTag[Double] => "0.0"
      case ct if ct == classTag[Float]  => "0.0f"
      case ct                           =>
        throw new RuntimeException(s"Unsupported numeric field type $ct")
    }
    Rule.ifSet(rules.getIgnoreEmpty)(ComparativeRulesGen.constRule(zero))
  }

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

      case Type.Any(_) =>
        None

      case Type.Map(mapRules) =>
        Rule.ifSet(mapRules.getIgnoreEmpty)(
          RepeatedRulesGen.maxItems(fd, 0, di)
        )

      case Type.Enum(_) =>
        None

      case _ => None
    }

}
