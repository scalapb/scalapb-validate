package scalapb.validate

import com.google.protobuf.Descriptors.FieldDescriptor
import io.envoyproxy.pgv.validate.Validate

object NumericRulesPrinter {

  // constant definition
  private val COMPARATIVE_VALIDATION = "io.envoyproxy.pgv.ComparativeValidation"
  private val CONSTANT_VALIDATION = "io.envoyproxy.pgv.ConstantValidation"

  private val JAVA_INT_PKG = "java.lang.Integer"
  private val JAVA_LONG_PKG = "java.lang.Long"
  private val JAVA_FLOAT_PKG = "java.lang.Float"
  private val JAVA_DOUBLE_PKG = "java.lang.Double"

  private def gt[T](
      fieldDescriptor: FieldDescriptor,
      inputExpr: String,
      defined: Boolean,
      value: T,
      typeParam: String
  ): Option[String] =
    if (defined)
      Some(
        s"""$COMPARATIVE_VALIDATION.greaterThan[$typeParam]("${fieldDescriptor.getName}", $inputExpr, $value, java.util.Comparator.naturalOrder)"""
      )
    else None

  private def gte[T](
      fieldDescriptor: FieldDescriptor,
      inputExpr: String,
      defined: Boolean,
      value: T,
      typeParam: String
  ): Option[String] =
    if (defined)
      Some(
        s"""$COMPARATIVE_VALIDATION.greaterThanOrEqual[$typeParam]("${fieldDescriptor.getName}", $inputExpr, $value, java.util.Comparator.naturalOrder)"""
      )
    else None

  private def lt[T](
      fieldDescriptor: FieldDescriptor,
      inputExpr: String,
      defined: Boolean,
      value: T,
      typeParam: String
  ): Option[String] =
    if (defined)
      Some(
        s"""$COMPARATIVE_VALIDATION.lessThan[$typeParam]("${fieldDescriptor.getName}", $inputExpr, $value, java.util.Comparator.naturalOrder)"""
      )
    else
      None

  private def lte[T](
      fieldDescriptor: FieldDescriptor,
      inputExpr: String,
      defined: Boolean,
      value: T,
      typeParam: String
  ): Option[String] =
    if (defined)
      Some(
        s"""$COMPARATIVE_VALIDATION.lessThanOrEqual[$typeParam]("${fieldDescriptor.getName}", $inputExpr, $value, java.util.Comparator.naturalOrder)"""
      )
    else None

  private def const[T](
      fieldDescriptor: FieldDescriptor,
      inputExpr: String,
      defined: Boolean,
      value: T,
      typeParam: String
  ): Option[String] =
    if (defined)
      Some(
        s"""$CONSTANT_VALIDATION.constant[$typeParam]("${fieldDescriptor.getName}", $inputExpr, $value)"""
      )
    else None

  private def toIterable(sq: Seq[Option[String]]): Iterable[String] = sq.flatten

  def printFixed32Rules(
      fieldDescriptor: FieldDescriptor,
      inputExpr: String,
      rules: Validate.Fixed32Rules
  ): Iterable[String] =
    toIterable(
      Seq(
        gt(fieldDescriptor, inputExpr, rules.hasGt, rules.getGt, JAVA_INT_PKG),
        gte(
          fieldDescriptor,
          inputExpr,
          rules.hasGte,
          rules.getGte,
          JAVA_INT_PKG
        ),
        lt(fieldDescriptor, inputExpr, rules.hasLt, rules.getLt, JAVA_INT_PKG),
        lte(
          fieldDescriptor,
          inputExpr,
          rules.hasLte,
          rules.getLte,
          JAVA_INT_PKG
        ),
        const(
          fieldDescriptor,
          inputExpr,
          rules.hasConst,
          rules.getConst,
          JAVA_INT_PKG
        )
      )
    )

  def printFixed64Rules(
      fieldDescriptor: FieldDescriptor,
      inputExpr: String,
      rules: Validate.Fixed64Rules
  ): Iterable[String] =
    toIterable(
      Seq(
        gt(fieldDescriptor, inputExpr, rules.hasGt, rules.getGt, JAVA_LONG_PKG),
        gte(
          fieldDescriptor,
          inputExpr,
          rules.hasGte,
          rules.getGte,
          JAVA_LONG_PKG
        ),
        lt(fieldDescriptor, inputExpr, rules.hasLt, rules.getLt, JAVA_LONG_PKG),
        lte(
          fieldDescriptor,
          inputExpr,
          rules.hasLte,
          rules.getLte,
          JAVA_LONG_PKG
        ),
        const(
          fieldDescriptor,
          inputExpr,
          rules.hasConst,
          rules.getConst,
          JAVA_LONG_PKG
        )
      )
    )

  def printSFixed32Rules(
      fieldDescriptor: FieldDescriptor,
      inputExpr: String,
      rules: Validate.SFixed32Rules
  ): Iterable[String] =
    toIterable(
      Seq(
        gt(fieldDescriptor, inputExpr, rules.hasGt, rules.getGt, JAVA_INT_PKG),
        gte(
          fieldDescriptor,
          inputExpr,
          rules.hasGte,
          rules.getGte,
          JAVA_INT_PKG
        ),
        lt(fieldDescriptor, inputExpr, rules.hasLt, rules.getLt, JAVA_INT_PKG),
        lte(
          fieldDescriptor,
          inputExpr,
          rules.hasLte,
          rules.getLte,
          JAVA_INT_PKG
        ),
        const(
          fieldDescriptor,
          inputExpr,
          rules.hasConst,
          rules.getConst,
          JAVA_INT_PKG
        )
      )
    )

  def printSFixed64Rules(
      fieldDescriptor: FieldDescriptor,
      inputExpr: String,
      rules: Validate.SFixed64Rules
  ): Iterable[String] =
    toIterable(
      Seq(
        gt(fieldDescriptor, inputExpr, rules.hasGt, rules.getGt, JAVA_LONG_PKG),
        gte(
          fieldDescriptor,
          inputExpr,
          rules.hasGte,
          rules.getGte,
          JAVA_LONG_PKG
        ),
        lt(fieldDescriptor, inputExpr, rules.hasLt, rules.getLt, JAVA_LONG_PKG),
        lte(
          fieldDescriptor,
          inputExpr,
          rules.hasLte,
          rules.getLte,
          JAVA_LONG_PKG
        ),
        const(
          fieldDescriptor,
          inputExpr,
          rules.hasConst,
          rules.getConst,
          JAVA_LONG_PKG
        )
      )
    )

  def printInt32Rules(
      fieldDescriptor: FieldDescriptor,
      inputExpr: String,
      rules: Validate.Int32Rules
  ): Iterable[String] =
    toIterable(
      Seq(
        gt(fieldDescriptor, inputExpr, rules.hasGt, rules.getGt, JAVA_INT_PKG),
        gte(
          fieldDescriptor,
          inputExpr,
          rules.hasGte,
          rules.getGte,
          JAVA_INT_PKG
        ),
        lt(fieldDescriptor, inputExpr, rules.hasLt, rules.getLt, JAVA_INT_PKG),
        lte(
          fieldDescriptor,
          inputExpr,
          rules.hasLte,
          rules.getLte,
          JAVA_INT_PKG
        ),
        const(
          fieldDescriptor,
          inputExpr,
          rules.hasConst,
          rules.getConst,
          JAVA_INT_PKG
        )
      )
    )

  def printInt64Rules(
      fieldDescriptor: FieldDescriptor,
      inputExpr: String,
      rules: Validate.Int64Rules
  ): Iterable[String] =
    toIterable(
      Seq(
        gt(fieldDescriptor, inputExpr, rules.hasGt, rules.getGt, JAVA_LONG_PKG),
        gte(
          fieldDescriptor,
          inputExpr,
          rules.hasGte,
          rules.getGte,
          JAVA_LONG_PKG
        ),
        lt(fieldDescriptor, inputExpr, rules.hasLt, rules.getLt, JAVA_LONG_PKG),
        lte(
          fieldDescriptor,
          inputExpr,
          rules.hasLte,
          rules.getLte,
          JAVA_LONG_PKG
        ),
        const(
          fieldDescriptor,
          inputExpr,
          rules.hasConst,
          rules.getConst,
          JAVA_LONG_PKG
        )
      )
    )

  def printSInt32Rules(
      fieldDescriptor: FieldDescriptor,
      inputExpr: String,
      rules: Validate.SInt32Rules
  ): Iterable[String] =
    toIterable(
      Seq(
        gt(fieldDescriptor, inputExpr, rules.hasGt, rules.getGt, JAVA_INT_PKG),
        gte(
          fieldDescriptor,
          inputExpr,
          rules.hasGte,
          rules.getGte,
          JAVA_INT_PKG
        ),
        lt(fieldDescriptor, inputExpr, rules.hasLt, rules.getLt, JAVA_INT_PKG),
        lte(
          fieldDescriptor,
          inputExpr,
          rules.hasLte,
          rules.getLte,
          JAVA_INT_PKG
        ),
        const(
          fieldDescriptor,
          inputExpr,
          rules.hasConst,
          rules.getConst,
          JAVA_INT_PKG
        )
      )
    )

  def printSInt64Rules(
      fieldDescriptor: FieldDescriptor,
      inputExpr: String,
      rules: Validate.SInt64Rules
  ): Iterable[String] =
    toIterable(
      Seq(
        gt(fieldDescriptor, inputExpr, rules.hasGt, rules.getGt, JAVA_LONG_PKG),
        gte(
          fieldDescriptor,
          inputExpr,
          rules.hasGte,
          rules.getGte,
          JAVA_LONG_PKG
        ),
        lt(fieldDescriptor, inputExpr, rules.hasLt, rules.getLt, JAVA_LONG_PKG),
        lte(
          fieldDescriptor,
          inputExpr,
          rules.hasLte,
          rules.getLte,
          JAVA_LONG_PKG
        ),
        const(
          fieldDescriptor,
          inputExpr,
          rules.hasConst,
          rules.getConst,
          JAVA_LONG_PKG
        )
      )
    )

  def printFloatRules(
      fieldDescriptor: FieldDescriptor,
      inputExpr: String,
      rules: Validate.FloatRules
  ): Iterable[String] =
    toIterable(
      Seq(
        gt(
          fieldDescriptor,
          inputExpr,
          rules.hasGt,
          rules.getGt,
          JAVA_FLOAT_PKG
        ),
        gte(
          fieldDescriptor,
          inputExpr,
          rules.hasGte,
          rules.getGte,
          JAVA_FLOAT_PKG
        ),
        lt(
          fieldDescriptor,
          inputExpr,
          rules.hasLt,
          rules.getLt,
          JAVA_FLOAT_PKG
        ),
        lte(
          fieldDescriptor,
          inputExpr,
          rules.hasLte,
          rules.getLte,
          JAVA_FLOAT_PKG
        ),
        const(
          fieldDescriptor,
          inputExpr,
          rules.hasConst,
          rules.getConst,
          JAVA_FLOAT_PKG
        )
      )
    )

  def printDoubleRules(
      fieldDescriptor: FieldDescriptor,
      inputExpr: String,
      rules: Validate.DoubleRules
  ): Iterable[String] =
    toIterable(
      Seq(
        gt(
          fieldDescriptor,
          inputExpr,
          rules.hasGt,
          rules.getGt,
          JAVA_DOUBLE_PKG
        ),
        gte(
          fieldDescriptor,
          inputExpr,
          rules.hasGte,
          rules.getGte,
          JAVA_DOUBLE_PKG
        ),
        lt(
          fieldDescriptor,
          inputExpr,
          rules.hasLt,
          rules.getLt,
          JAVA_DOUBLE_PKG
        ),
        lte(
          fieldDescriptor,
          inputExpr,
          rules.hasLte,
          rules.getLte,
          JAVA_DOUBLE_PKG
        ),
        const(
          fieldDescriptor,
          inputExpr,
          rules.hasConst,
          rules.getConst,
          JAVA_DOUBLE_PKG
        )
      )
    )

  def printUint64Rules(
      fieldDescriptor: FieldDescriptor,
      inputExpr: String,
      rules: Validate.UInt64Rules
  ): Iterable[String] =
    toIterable(
      Seq(
        gt(fieldDescriptor, inputExpr, rules.hasGt, rules.getGt, JAVA_LONG_PKG),
        gte(
          fieldDescriptor,
          inputExpr,
          rules.hasGte,
          rules.getGte,
          JAVA_LONG_PKG
        ),
        lt(fieldDescriptor, inputExpr, rules.hasLt, rules.getLt, JAVA_LONG_PKG),
        lte(
          fieldDescriptor,
          inputExpr,
          rules.hasLte,
          rules.getLte,
          JAVA_LONG_PKG
        ),
        const(
          fieldDescriptor,
          inputExpr,
          rules.hasConst,
          rules.getConst,
          JAVA_LONG_PKG
        )
      )
    )

  def printUint32Rules(
      fieldDescriptor: FieldDescriptor,
      inputExpr: String,
      rules: Validate.UInt32Rules
  ): Iterable[String] =
    toIterable(
      Seq(
        gt(fieldDescriptor, inputExpr, rules.hasGt, rules.getGt, JAVA_INT_PKG),
        gte(
          fieldDescriptor,
          inputExpr,
          rules.hasGte,
          rules.getGte,
          JAVA_INT_PKG
        ),
        lt(fieldDescriptor, inputExpr, rules.hasLt, rules.getLt, JAVA_INT_PKG),
        lte(
          fieldDescriptor,
          inputExpr,
          rules.hasLte,
          rules.getLte,
          JAVA_INT_PKG
        ),
        const(
          fieldDescriptor,
          inputExpr,
          rules.hasConst,
          rules.getConst,
          JAVA_INT_PKG
        )
      )
    )
}
