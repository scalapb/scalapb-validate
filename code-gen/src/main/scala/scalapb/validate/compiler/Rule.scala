package scalapb.validate.compiler

import com.google.protobuf.Descriptors.FieldDescriptor

sealed trait Rule {
  def render(descriptor: FieldDescriptor, input: String): String
}

case class BasicRule(funcName: String, args: Seq[String]) extends Rule {
  self =>
  def render(descriptor: FieldDescriptor, input: String): String = {
    val base = s"""$funcName("${descriptor.getName()}", $input"""
    if (args.isEmpty) base + ")"
    else base + args.mkString(", ", ", ", ")")
  }

  def wrapJava: Rule =
    new Rule {
      def render(descriptor: FieldDescriptor, input: String): String =
        s"scalapb.validate.Result.run(${self.render(descriptor, input)})"
    }
}

object Rule {
  def basic(funcName: String, args: String*): BasicRule =
    BasicRule(funcName, args)

  def java(funcName: String, args: String*): Rule =
    basic(funcName, args: _*).wrapJava

  def ifSet[T](cond: => Boolean)(value: => T): Option[T] =
    if (cond) Some(value) else None
}
