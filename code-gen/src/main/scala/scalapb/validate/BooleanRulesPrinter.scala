package scalapb.validate

import com.google.protobuf.Descriptors.FieldDescriptor
import io.envoyproxy.pgv.validate.Validate.BoolRules

object BooleanRulesPrinter {
  private val CONSTANT_VALIDATION = "io.envoyproxy.pgv.ConstantValidation"
  private val JAVA_BOOLEAN_PKG = "java.lang.Boolean"

  def print(
      fieldDescriptor: FieldDescriptor,
      inputExpr: String,
      rules: BoolRules
  ): Iterable[String] =
    if (rules.hasConst)
      Some(
        s"""$CONSTANT_VALIDATION.constant[$JAVA_BOOLEAN_PKG]("${fieldDescriptor.getName}", $inputExpr, ${rules.getConst})"""
      )
    else None
}
