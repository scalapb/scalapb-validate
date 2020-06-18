package scalapb.validate.compiler

import com.google.protobuf.Descriptors.FieldDescriptor
import scalapb.compiler.Expression
import scalapb.compiler.Identity
import scalapb.compiler.ExpressionBuilder
import scalapb.compiler.EnclosingType
import scalapb.compiler.FunctionApplication

/** Represents a generated function call that returns a Result.
  *
  * funcName: fully qualified functin name. The function takes potentially the name
  *           (if needsName is true),  then the value to be tested, then the list of
  *           args are passed:
  *           funcName([name], value, *args)
  * needsName: whether the first argument is the name of the field being tested.
  * args: arguments to pass after the value.
  * inputTransform: transformation to apply to the value
  * outputTransform: transformation to apply to the result of funcName
  * imports: list of imports to add to the top of the file (no need to dedupe here)
  * preamble: code to be added for static definitions. Can be used for constants that
  *           needs to be computed only once.
  */
case class Rule(
    funcName: String,
    needsName: Boolean,
    args: Seq[String],
    inputTransform: Expression,
    outputTranform: Expression,
    imports: Seq[String],
    preamble: Seq[String]
) {
  self =>
  def render(descriptor: FieldDescriptor, input: String): String = {
    val e =
      ExpressionBuilder.run(inputTransform)(input, EnclosingType.None, false)
    val maybeName = if (needsName) s""""${descriptor.getName()}", """ else ""
    val base = s"""$funcName(${maybeName}${e}"""
    val out =
      if (args.isEmpty) base + ")"
      else base + args.mkString(", ", ", ", ")")
    ExpressionBuilder.run(outputTranform)(out, EnclosingType.None, false)
  }

  def wrapJava: Rule =
    copy(outputTranform =
      FunctionApplication("scalapb.validate.Result.run") andThen outputTranform
    )

  def withImport(name: String) = copy(imports = imports :+ name)

  def withPreamble(lines: String*) = copy(preamble = preamble ++ lines)
}

object Rule {
  def basic(
      funcName: String,
      args: Seq[String],
      inputTransform: Expression = Identity
  ): Rule =
    Rule(funcName, true, args, inputTransform, Identity, Nil, Nil)

  def basic(funcName: String, args: String*): Rule =
    basic(funcName, args)

  def java(
      funcName: String,
      args: Seq[String],
      inputTransform: Expression
  ): Rule =
    basic(funcName, args, inputTransform).wrapJava

  def java(funcName: String, args: String*): Rule =
    basic(funcName, args, Identity).wrapJava

  def messageValidate(validator: String): Rule =
    Rule(s"$validator.validate", false, Seq.empty, Identity, Identity, Nil, Nil)

  def ifSet[T](cond: => Boolean)(value: => T): Option[T] =
    if (cond) Some(value) else None
}
