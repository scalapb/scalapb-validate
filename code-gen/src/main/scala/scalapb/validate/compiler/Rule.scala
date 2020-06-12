package scalapb.validate.compiler

import com.google.protobuf.Descriptors.FieldDescriptor
import scalapb.compiler.Expression
import scalapb.compiler.Identity
import scalapb.compiler.ExpressionBuilder
import scalapb.compiler.EnclosingType
import scalapb.compiler.FunctionApplication

case class Rule(
    funcName: String,
    args: Seq[String],
    needsName: Boolean,
    inputTransform: Expression,
    outputTranform: Expression,
    imports: Seq[String]
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
}

object Rule {
  def basic(
      funcName: String,
      args: Seq[String],
      transform: Expression = Identity
  ): Rule =
    Rule(funcName, args, true, transform, Identity, Nil)

  def basic(funcName: String, arg1: String): Rule =
    basic(funcName, Seq(arg1))

  def basic(funcName: String): Rule =
    basic(funcName, Seq.empty)

  def java(funcName: String, transform: Expression, args: String*): Rule =
    basic(funcName, args, transform).wrapJava

  def java(funcName: String, args: String*): Rule =
    basic(funcName, args, Identity).wrapJava

  def messageValidate(validator: String): Rule =
    Rule(s"$validator.validate", Seq.empty, false, Identity, Identity, Nil)

  def ifSet[T](cond: => Boolean)(value: => T): Option[T] =
    if (cond) Some(value) else None
}
