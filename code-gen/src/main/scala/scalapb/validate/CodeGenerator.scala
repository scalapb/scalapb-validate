package scalapb.validate

import com.google.protobuf.Descriptors.{Descriptor, FieldDescriptor}
import com.google.protobuf.ExtensionRegistry
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse
import io.envoyproxy.pgv.validate.Validate
import io.envoyproxy.pgv.validate.Validate.FieldRules.TypeCase
import io.envoyproxy.pgv.validate.Validate.{FieldRules, MessageRules}
import protocbridge.Artifact
import protocbridge.codegen.{CodeGenApp, CodeGenRequest, CodeGenResponse}
import scalapb.compiler.{
  DescriptorImplicits,
  FunctionalPrinter,
  ProtobufGenerator
}
import scalapb.options.compiler.Scalapb
import scalapb.validate.compat.JavaConverters._

object CodeGenerator extends CodeGenApp {
  override def registerExtensions(registry: ExtensionRegistry): Unit = {
    Scalapb.registerAllExtensions(registry)
    Validate.registerAllExtensions(registry)
  }

  override def suggestedDependencies: Seq[Artifact] =
    Seq(
      Artifact(
        "com.thesamet.scalapb",
        "scalapb-validate-core",
        BuildInfo.version,
        crossVersion = true
      )
    )

  def process(request: CodeGenRequest): CodeGenResponse =
    ProtobufGenerator.parseParameters(request.parameter) match {
      case Right(params) =>
        val implicits =
          new DescriptorImplicits(params, request.allProtos)
        CodeGenResponse.succeed(
          for {
            file <- request.filesToGenerate
            message <- file.getMessageTypes().asScala
          } yield new MessagePrinter(implicits, message).result()
        )
      case Left(error) =>
        CodeGenResponse.fail(error)
    }
}

object MessagePrinter {}

class MessagePrinter(
    implicits: DescriptorImplicits,
    message: Descriptor
) {
  import DescriptorImplicits.AsSymbolPimp
  import implicits._

  private def validatorName(fd: Descriptor): ScalaName =
    if (fd.isTopLevel)
      message.scalaType.sibling(message.scalaType.name + "Validator")
    else
      validatorName(fd.getContainingType) / (fd.scalaType.name + "Validator")

  private val objectName = validatorName(message)

  def scalaFileName: String =
    message.getFile.scalaDirectory + "/" + objectName.name + ".scala"

  private val Validator = "scalapb.validate.Validator"
  private val Result = "scalapb.validate.Result"

  def printValidate(fp: FunctionalPrinter): FunctionalPrinter =
    fp.add(
        s"def validate(input: ${message.scalaType.fullName}): $Result = $Result.run {"
      )
      .indented(
        _.print(message.getFields.asScala)(printField(_, _))
      )
      .add("}")
      .print(message.getNestedTypes.asScala)((fp, fd) =>
        new MessagePrinter(implicits, fd).printObject(fp)
      )

  def printObject(fp: FunctionalPrinter): FunctionalPrinter =
    fp.add(
        s"object ${objectName.name} extends $Validator[${message.scalaType.fullName}] {"
      )
      .indented(
        _.call(printValidate)
      )
      .add("}")

  def content: String = {
    val fp = new FunctionalPrinter()
    fp.add(s"package ${message.getFile.scalaPackage.fullName}", "")
      .add()
      .call(printObject)
      .result()
  }

  def messageRules(
      fieldDescriptor: FieldDescriptor,
      inputExpr: String,
      rules: MessageRules
  ): Iterable[String] = {
    val name = fieldDescriptor.getName

    val maybeRequired =
      if (rules.getRequired)
        Some(
          s"""if ($inputExpr.isEmpty) throw new io.envoyproxy.pgv.ValidationException("$name", "None", "is required")"""
        )
      else None

    val inner = if (!rules.getSkip) {
      val msgType = fieldDescriptor.getMessageType
      val cmpName = validatorName(msgType).fullName
      // doesn't work correctly yet - since validate doesn't throw an exception
      Some(
        s"""$inputExpr.foreach($cmpName.validate)"""
      )
    } else None

    maybeRequired ++ inner
  }

  def rulesSingle(
      fd: FieldDescriptor,
      input: String,
      rules: FieldRules
  ): Iterable[String] =
    rules.getTypeCase match {
      case TypeCase.STRING =>
        StringRulesPrinter.print(fd, input, rules.getString)

      case TypeCase.UINT64 =>
        NumericRulesPrinter.printUint64Rules(fd, input, rules.getUint64)

      case TypeCase.UINT32 =>
        NumericRulesPrinter.printUint32Rules(fd, input, rules.getUint32)

      case TypeCase.INT64 =>
        NumericRulesPrinter.printInt64Rules(fd, input, rules.getInt64)

      case TypeCase.INT32 =>
        NumericRulesPrinter.printInt32Rules(fd, input, rules.getInt32)

      case TypeCase.SINT64 =>
        NumericRulesPrinter.printSInt64Rules(fd, input, rules.getSint64)

      case TypeCase.SINT32 =>
        NumericRulesPrinter.printSInt32Rules(fd, input, rules.getSint32)

      case TypeCase.FIXED64 =>
        NumericRulesPrinter.printFixed64Rules(fd, input, rules.getFixed64)

      case TypeCase.FIXED32 =>
        NumericRulesPrinter.printFixed32Rules(fd, input, rules.getFixed32)

      case TypeCase.SFIXED64 =>
        NumericRulesPrinter.printSFixed64Rules(fd, input, rules.getSfixed64)

      case TypeCase.SFIXED32 =>
        NumericRulesPrinter.printSFixed32Rules(fd, input, rules.getSfixed32)

      case TypeCase.BOOL =>
        BooleanRulesPrinter.print(fd, input, rules.getBool)

      case TypeCase.TYPE_NOT_SET =>
        if (fd.isMessage && rules.hasMessage)
          messageRules(fd, input, rules.getMessage)
        else Seq.empty
      case _ => Seq.empty
    }

  def printField(
      fp: FunctionalPrinter,
      fd: FieldDescriptor
  ): FunctionalPrinter = {
    val rules = fd.getOptions.getExtension(Validate.rules)
    val accessor = s"input.${fd.scalaName.asSymbol}"
    if (fd.supportsPresence && !fd.isMessage) {
      val validations = rulesSingle(fd, "_value", rules).toSeq
      if (validations.nonEmpty)
        fp.add(s"$accessor.foreach { _value =>")
          .indented(
            _.add(validations: _*)
          )
          .add("}")
      else fp
    } else
      fp.add(rulesSingle(fd, s"${accessor}", rules).toSeq: _*)
  }

  def result(): CodeGeneratorResponse.File = {
    val b = CodeGeneratorResponse.File.newBuilder()
    b.setName(scalaFileName)
    b.setContent(content)
    b.build()
  }
}
