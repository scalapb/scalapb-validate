package scalapb.validate

import com.google.protobuf.ExtensionRegistry
import scalapb.options.compiler.Scalapb
import scalapb.compiler.ProtobufGenerator
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse
import protocbridge.Artifact
import scalapb.validate.compat.JavaConverters._
import scalapb.compiler.DescriptorImplicits
import com.google.protobuf.Descriptors.Descriptor
import scalapb.compiler.FunctionalPrinter
import protocbridge.codegen.CodeGenApp
import protocbridge.codegen.CodeGenResponse
import protocbridge.codegen.CodeGenRequest
import com.google.protobuf.Descriptors.FieldDescriptor
import io.envoyproxy.pgv.validate.Validate
import io.envoyproxy.pgv.validate.Validate.FieldRules.TypeCase
import io.envoyproxy.pgv.validate.Validate.StringRules
import io.envoyproxy.pgv.validate.Validate.MessageRules
import io.envoyproxy.pgv.validate.Validate.UInt64Rules
import io.envoyproxy.pgv.validate.Validate.FieldRules

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
  import implicits._
  import DescriptorImplicits.AsSymbolPimp

  private def validatorName(fd: Descriptor): ScalaName =
    if (fd.isTopLevel)
      message.scalaType.sibling(message.scalaType.name + "Validator")
    else
      validatorName(fd.getContainingType()) / (fd.scalaType.name + "Validator")

  private val objectName = validatorName(message)

  def scalaFileName =
    message.getFile.scalaDirectory + "/" + objectName.name + ".scala"

  private val Validator = "scalapb.validate.Validator"
  private val Result = "scalapb.validate.Result"

  def printValidate(fp: FunctionalPrinter): FunctionalPrinter =
    fp.add(
        s"def validate(input: ${message.scalaType.fullName}): $Result = $Result.run {"
      )
      .indented(
        _.print(message.getFields().asScala)(printField(_, _))
      )
      .add("}")
      .print(message.getNestedTypes().asScala)((fp, fd) =>
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

  def stringRules(
      fieldDescriptor: FieldDescriptor,
      inputExpr: String,
      rules: StringRules
  ): Iterable[String] = {
    val name = fieldDescriptor.getName()
    val SV = "io.envoyproxy.pgv.StringValidation"
    val maybeLen =
      if (rules.hasLen())
        Option(
          s"""$SV.length("$name", $inputExpr, ${rules.getLen()}")"""
        )
      else None

    val maybeLenBytes =
      if (rules.hasLenBytes())
        Option(
          s"""$SV.lengthBytes("$name", $inputExpr, ${rules.getLenBytes()}")"""
        )
      else None

    val maybeEmail =
      if (rules.getEmail())
        Option(
          s"""$SV.email("$name", $inputExpr)"""
        )
      else None

    // Need to find way to compile the pattern only once
    val maybePattern = if (rules.hasPattern())
      Option(
        s"""$SV.pattern("$name", $inputExpr, com.google.re2j.Pattern.compile("${rules.getPattern}"))"""
      )
    else None

    maybeLen ++ maybeLenBytes ++ maybeEmail ++ maybePattern
  }

  def uint64Rules(
      fieldDescriptor: FieldDescriptor,
      inputExpr: String,
      rules: UInt64Rules
  ): Iterable[String] = {
    val name = fieldDescriptor.getName()
    val CV = "io.envoyproxy.pgv.ComparativeValidation"
    val maybeGt =
      if (rules.hasGt())
        Option(
          s"""$CV.greaterThan[java.lang.Long]("$name", $inputExpr, ${rules
            .getGt()}, java.util.Comparator.naturalOrder)"""
        )
      else None

    maybeGt
  }

  def messageRules(
      fieldDescriptor: FieldDescriptor,
      inputExpr: String,
      rules: MessageRules
  ): Iterable[String] = {
    val name = fieldDescriptor.getName()

    val maybeRequired =
      if (rules.getRequired())
        Some(
          s"""if ($inputExpr.isEmpty) throw new io.envoyproxy.pgv.ValidationException("$name", "None", "is required")"""
        )
      else None

    val inner = if (!rules.getSkip()) {
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
    rules.getTypeCase() match {
      case TypeCase.STRING => stringRules(fd, input, rules.getString())
      case TypeCase.UINT64 => uint64Rules(fd, input, rules.getUint64())
      case TypeCase.TYPE_NOT_SET =>
        if (fd.isMessage && rules.hasMessage())
          messageRules(fd, input, rules.getMessage())
        else Seq.empty
      case _ => Seq.empty
    }

  def printField(
      fp: FunctionalPrinter,
      fd: FieldDescriptor
  ): FunctionalPrinter = {
    val rules = fd.getOptions().getExtension(Validate.rules)
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
