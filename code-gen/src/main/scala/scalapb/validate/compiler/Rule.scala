package scalapb.validate.compiler

import com.google.protobuf.Descriptors.FieldDescriptor
import scalapb.compiler.Expression
import scalapb.compiler.Identity
import scalapb.compiler.EnclosingType
import scalapb.compiler.FunctionApplication
import scalapb.compiler.FunctionalPrinter
import scalapb.compiler.FunctionalPrinter.PrinterEndo

trait Rule {
  def preamble: Seq[String]
  def imports: Seq[String]
  def render(descriptor: FieldDescriptor, input: String): PrinterEndo
}

/** Represents a generated function call that returns a Result.
  *
  * funcName: fully qualified functin name. The function takes potentially the
  * name (if needsName is true), then the value to be tested, then the list of
  * args are passed: funcName([name], value, *args) needsName: whether the first
  * argument is the name of the field being tested. args: arguments to pass
  * after the value. inputTransform: transformation to apply to the value
  * outputTransform: transformation to apply to the result of funcName imports:
  * list of imports to add to the top of the file (no need to dedupe here)
  * preamble: code to be added for static definitions. Can be used for constants
  * that needs to be computed only once.
  */
case class FunctionCall(
    funcName: String,
    needsName: Boolean,
    args: Seq[String],
    inputTransform: Expression,
    outputTranform: Expression,
    imports: Seq[String],
    preamble: Seq[String]
) extends Rule {
  self =>
  def render(descriptor: FieldDescriptor, input: String): PrinterEndo = {
    val e =
      inputTransform(input, EnclosingType.None, false)
    val maybeName =
      if (needsName) s""""${Rule.getFullNameWithoutPackage(descriptor)}", """
      else ""
    val base = s"""$funcName($maybeName$e"""
    val out =
      if (args.isEmpty) base + ")"
      else base + args.mkString(", ", ", ", ")")
    _.add(outputTranform(out, EnclosingType.None, false))
  }

  def wrapJava: FunctionCall =
    copy(outputTranform =
      FunctionApplication("scalapb.validate.Result.run") andThen outputTranform
    )

  def withImport(name: String) = copy(imports = imports :+ name)

  def withPreamble(lines: String*) = copy(preamble = preamble ++ lines)
}

case class CombineFieldRules(rules: Seq[Rule], op: String = "&&") extends Rule {
  def preamble: Seq[String] = rules.flatMap(_.preamble)
  def imports: Seq[String] = rules.flatMap(_.imports)

  def render(descriptor: FieldDescriptor, input: String): PrinterEndo = {
    val lines = rules.map { r =>
      val fp = FunctionalPrinter()
      r.render(descriptor, input)(fp).content
    }
    _.addGroupsWithDelimiter(op)(lines)
  }
}

case class IgnoreEmptyRule(isEmpty: Rule, other: Rule) extends Rule {
  def preamble: Seq[String] = isEmpty.preamble ++ other.preamble
  def imports: Seq[String] = isEmpty.imports ++ other.imports
  def render(descriptor: FieldDescriptor, input: String): PrinterEndo =
    _.add("(")
      .indented(
        _.call(isEmpty.render(descriptor, input))
          .add(" ||")
          .call(other.render(descriptor, input))
          .add(")")
      )
}

case class OptionalFieldRule(rules: Seq[Rule]) extends Rule {
  def render(descriptor: FieldDescriptor, input: String): PrinterEndo = {
    val lines = rules.map { r =>
      val fp = FunctionalPrinter()
      r.render(descriptor, "_value")(fp).content
    }

    _.add(
      s"scalapb.validate.Result.optional($input) { _value =>"
    ).indented(_.addGroupsWithDelimiter(" &&")(lines))
      .add("}")
  }

  def preamble: Seq[String] = rules.flatMap(_.preamble)
  def imports: Seq[String] = rules.flatMap(_.imports)
}

case class RepeatedFieldRule(rules: Seq[Rule], inputTransform: String => String)
    extends Rule {
  def render(descriptor: FieldDescriptor, input: String): PrinterEndo = {
    val lines = rules.map { r =>
      val fp = FunctionalPrinter()
      r.render(descriptor, "_value")(fp).content
    }
    _.add(
      s"scalapb.validate.Result.repeated(${inputTransform(input)}) { _value =>"
    ).indented(_.addGroupsWithDelimiter(" &&")(lines))
      .add("}")
  }

  def preamble: Seq[String] = rules.flatMap(_.preamble)
  def imports: Seq[String] = rules.flatMap(_.imports)
}

object Rule {
  def basic(
      funcName: String,
      args: Seq[String],
      inputTransform: Expression = Identity
  ): FunctionCall =
    FunctionCall(funcName, true, args, inputTransform, Identity, Nil, Nil)

  def basic(funcName: String, args: String*): FunctionCall =
    basic(funcName, args)

  def java(
      funcName: String,
      args: Seq[String],
      inputTransform: Expression
  ): FunctionCall =
    basic(funcName, args, inputTransform).wrapJava

  def java(funcName: String, args: String*): FunctionCall =
    basic(funcName, args, Identity).wrapJava

  def messageValidate(validator: String): Rule =
    FunctionCall(
      s"$validator.validate",
      false,
      Seq.empty,
      Identity,
      Identity,
      Nil,
      Nil
    )

  def ifSet[T](cond: => Boolean)(value: => T): Option[T] =
    if (cond) Some(value) else None

  private[compiler] def getFullNameWithoutPackage(
      descriptor: FieldDescriptor
  ): String = {
    val fullName = descriptor.getFullName
    val packageName = descriptor.getFile.getPackage
    if (packageName.isEmpty)
      fullName
    else
      fullName.substring(packageName.length + 1)
  }
}
