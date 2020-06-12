package scalapb.validate.compiler

import com.google.protobuf.Descriptors.FieldDescriptor
import scalapb.compiler.Expression
import scalapb.compiler.Identity
import scalapb.compiler.ExpressionBuilder
import scalapb.compiler.EnclosingType

sealed trait Rule {
  def render(descriptor: FieldDescriptor, input: String): String
}

case class BasicRule(funcName: String, args: Seq[String], transform: Expression)
    extends Rule {
  self =>
  def render(descriptor: FieldDescriptor, input: String): String = {
    val e = ExpressionBuilder.run(transform)(input, EnclosingType.None, false)
    val base = s"""$funcName("${descriptor.getName()}", ${e}"""
    if (args.isEmpty) base + ")"
    else base + args.mkString(", ", ", ", ")")
  }

  def wrapJava: Rule =
    new Rule {
      def render(descriptor: FieldDescriptor, input: String): String =
        s"scalapb.validate.Result.run(${self.render(descriptor, input)})"
    }
}

case class MessageValidateRule(validatorName: String) extends Rule {
  def render(descriptor: FieldDescriptor, input: String): String =
    s"$validatorName.validate($input)"
}

object Rule {
  def basic(
      funcName: String,
      args: Seq[String],
      transform: Expression = Identity
  ): BasicRule =
    BasicRule(funcName, args, transform)

  def basic(funcName: String, arg1: String): BasicRule =
    basic(funcName, Seq(arg1))

  def basic(funcName: String): BasicRule =
    basic(funcName, Seq.empty)

  def java(funcName: String, args: String*): Rule =
    basic(funcName, args).wrapJava

  def messageValidate(validator: String): Rule =
    MessageValidateRule(validator)

  def ifSet[T](cond: => Boolean)(value: => T): Option[T] =
    if (cond) Some(value) else None
}
