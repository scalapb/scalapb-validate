package scalapb.validate

import com.google.protobuf.Descriptors.{Descriptor, FieldDescriptor}
import com.google.protobuf.ExtensionRegistry
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse
import io.envoyproxy.pgv.validate.Validate
import io.envoyproxy.pgv.validate.Validate.FieldRules.TypeCase
import io.envoyproxy.pgv.validate.Validate.{
  FieldRules,
  MessageRules,
  StringRules,
  UInt64Rules
}
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

    val maybePrefix =
      if (rules.hasPrefix())
        Option(s"""$SV.prefix("$name", $inputExpr, ${rules.getPrefix})""")
      else None

    val maybeSuffix =
      if (rules.hasSuffix())
        Option(s"""$SV.suffix("$name", $inputExpr, ${rules.getSuffix})""")
      else None

    val maybeContains =
      if (rules.hasContains)
        Option(s"""$SV.contains("$name", $inputExpr, ${rules.getContains})""")
      else None

    val maybeNotContains =
      if (rules.hasNotContains)
        Option(
          s"""$SV.notContains("$name", $inputExpr, ${rules.getNotContains})"""
        )
      else None

    val maybeHostName =
      if (rules.getHostname)
        Option(
          s"""$SV.hostName("$name", $inputExpr)"""
        )
      else None

    val maybeIp =
      if (rules.getIp)
        Option(
          s"""$SV.ip("$name", $inputExpr)"""
        )
      else None

    val maybeIpv4 =
      if (rules.getIpv4)
        Option(
          s"""$SV.ipv4("$name", $inputExpr)"""
        )
      else None

    val maybeIpv6 =
      if (rules.getIpv6)
        Option(
          s"""$SV.ipv6("$name", $inputExpr)"""
        )
      else None

    val maybeUri =
      if (rules.getUri)
        Option(
          s"""$SV.uri("$name", $inputExpr)"""
        )
      else None

    val maybeUriRef =
      if (rules.getUriRef)
        Option(
          s"""$SV.uriRef("$name", $inputExpr)"""
        )
      else None

    val maybeUuid =
      if (rules.getUuid)
        Option(
          s"""$SV.uuid("$name", $inputExpr)"""
        )
      else None

    val maybeAddress =
      if (rules.getAddress)
        Option(
          s"""$SV.address("$name", $inputExpr)"""
        )
      else None

    val maybeMaxBytes =
      if (rules.hasMaxBytes)
        Option(
          s"""$SV.maxBytes("$name", $inputExpr, ${rules.getMaxBytes})"""
        )
      else None

    val maybeMinBytes =
      if (rules.hasMinBytes)
        Option(
          s"""$SV.minBytes("$name", $inputExpr, ${rules.getMinBytes})"""
        )
      else None

    val maybeMiniLength =
      if (rules.hasMinLen)
        Option(
          s"""$SV.minLength("$name", $inputExpr, ${rules.getMinLen})"""
        )
      else None

    val maybeMaxLength =
      if (rules.hasMaxLen)
        Option(
          s"""$SV.maxLength("$name", $inputExpr, ${rules.getMaxLen})"""
        )
      else None

    maybeAddress
      .concat(maybeContains)
      .concat(maybeEmail)
      .concat(maybeHostName)
      .concat(maybeIp)
      .concat(maybeIpv4)
      .concat(maybeIpv6)
      .concat(maybeLen)
      .concat(maybeLenBytes)
      .concat(maybeMaxBytes)
      .concat(maybeMaxLength)
      .concat(maybeMinBytes)
      .concat(maybeMiniLength)
      .concat(maybeNotContains)
      .concat(maybePattern)
      .concat(maybePrefix)
      .concat(maybeSuffix)
      .concat(maybeUuid)
      .concat(maybeUriRef)
      .concat(maybeUri)
  }

  def uint64Rules(
      fieldDescriptor: FieldDescriptor,
      inputExpr: String,
      rules: UInt64Rules
  ): Iterable[String] = {
    val name = fieldDescriptor.getName
    val CV = "io.envoyproxy.pgv.ComparativeValidation"
    val maybeGt =
      if (rules.hasGt)
        Option(
          s"""$CV.greaterThan[java.lang.Long]("$name", $inputExpr, ${rules.getGt}, java.util.Comparator.naturalOrder)"""
        )
      else None

    maybeGt
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
      case TypeCase.STRING => stringRules(fd, input, rules.getString)
      case TypeCase.UINT64 => uint64Rules(fd, input, rules.getUint64)
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
