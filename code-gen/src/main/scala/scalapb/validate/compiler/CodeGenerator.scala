package scalapb.validate.compiler

import com.google.protobuf.Descriptors.{Descriptor, FieldDescriptor}
import com.google.protobuf.ExtensionRegistry
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse
import io.envoyproxy.pgv.validate.Validate
import io.envoyproxy.pgv.validate.validate.FieldRules.Type
import io.envoyproxy.pgv.validate.validate.FieldRules
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
      fd.scalaType.sibling(fd.scalaType.name + "Validator")
    else
      validatorName(fd.getContainingType) / (fd.scalaType.name + "Validator")

  private val objectName = validatorName(message)

  def scalaFileName: String =
    message.getFile.scalaDirectory + "/" + objectName.name + ".scala"

  private val Validator = "scalapb.validate.Validator"
  private val Result = "scalapb.validate.Result"

  def printValidate(fp: FunctionalPrinter): FunctionalPrinter = {
    val ruleGroups =
      message.getFields.asScala.toSeq.flatMap(formattedRulesForField)
    fp.add(
        s"def validate(input: ${message.scalaType.fullName}): $Result ="
      )
      .indented(
        _.addGroupsWithDelimiter(" &&")(ruleGroups)
      )
      .when(ruleGroups.isEmpty)(_.add("  scalapb.validate.Success"))
      .add("")
      .print(message.getNestedTypes.asScala)((fp, fd) =>
        new MessagePrinter(implicits, fd).printObject(fp)
      )
  }

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

  private val SCALA_INT = "scala.Int"
  private val SCALA_LONG = "scala.Long"
  private val SCALA_FLOAT = "scala.Float"
  private val SCALA_DOUBLE = "scala.Double"

  def rulesSingle(
      rules: FieldRules
  ): Seq[Rule] =
    rules.`type` match {
      case Type.String(stringRules) =>
        StringRulesGen.print(stringRules)

      case Type.Uint64(numericRules) =>
        NumericRulesGen.numericRules(SCALA_LONG, numericRules)

      case Type.Sint64(numericRules) =>
        NumericRulesGen.numericRules(SCALA_LONG, numericRules)

      case Type.Sfixed64(numericRules) =>
        NumericRulesGen.numericRules(SCALA_LONG, numericRules)

      case Type.Int64(numericRules) =>
        NumericRulesGen.numericRules(SCALA_LONG, numericRules)

      case Type.Uint32(numericRules) =>
        NumericRulesGen.numericRules(SCALA_INT, numericRules)

      case Type.Sint32(numericRules) =>
        NumericRulesGen.numericRules(SCALA_INT, numericRules)

      case Type.Sfixed32(numericRules) =>
        NumericRulesGen.numericRules(SCALA_INT, numericRules)

      case Type.Int32(numericRules) =>
        NumericRulesGen.numericRules(SCALA_INT, numericRules)

      case Type.Double(numericRules) =>
        NumericRulesGen.numericRules(SCALA_DOUBLE, numericRules)

      case Type.Float(numericRules) =>
        NumericRulesGen.numericRules(SCALA_FLOAT, numericRules)

      case Type.Bool(boolRulesGen) =>
        BooleanRulesGen.booleanRules(boolRulesGen)

      case _ => Seq.empty
    }

  sealed trait RenderedResult

  case class SingularResult(line: String) extends RenderedResult

  case class OptionalResult(accessor: String, lines: Seq[String])
      extends RenderedResult

  def renderedRulesForField(fd: FieldDescriptor): Seq[RenderedResult] = {
    val rulesProto =
      FieldRules.fromJavaProto(fd.getOptions.getExtension(Validate.rules))
    val accessor =
      if (!fd.isInOneof) s"input.${fd.scalaName.asSymbol}"
      else
        s"input.${fd.getContainingOneof.scalaName.nameSymbol}.${fd.scalaName}"

    val rules = rulesSingle(rulesProto)

    val maybeOpt =
      if ((fd.isInOneof || fd.supportsPresence) && rules.nonEmpty)
        Seq(
          OptionalResult(
            accessor,
            rules.map(r => "  " + r.render(fd, "_value"))
          )
        )
      else Seq.empty

    val messageRules = if (fd.isMessage) {
      val maybeRequired =
        Rule.ifSet(fd.supportsPresence && rulesProto.getMessage.getRequired)(
          SingularResult(s"""scalapb.validate.RequiredValidation("${fd
            .getName()}", ${accessor})""")
        )

      val maybeNested = Rule.ifSet(
        fd.supportsPresence && !rulesProto.getMessage.getSkip &&
          !fd.getMessageType.getFullName.startsWith("google.protobuf")
      )(
        OptionalResult(
          accessor,
          Seq(validatorName(fd.getMessageType).fullName + ".validate(_value)")
        )
      )

      maybeRequired ++ maybeNested
    } else Seq.empty

    val maybeSingular =
      if (!fd.supportsPresence && !fd.isInOneof && rules.nonEmpty)
        rules.map(r => SingularResult(r.render(fd, accessor)))
      else Seq.empty

    maybeSingular ++ maybeOpt ++ messageRules
  }

  def formattedRulesForField(
      fd: FieldDescriptor
  ): Seq[Seq[String]] =
    renderedRulesForField(fd).map {
      case SingularResult(line) =>
        Seq(line)
      case OptionalResult(accessor, lines) =>
        Seq(s"scalapb.validate.Result.optional($accessor) { _value =>") ++
          lines.dropRight(1).map(l => l + " &&") ++
          Seq(lines.last, "}")
    }

  def result(): CodeGeneratorResponse.File = {
    val b = CodeGeneratorResponse.File.newBuilder()
    b.setName(scalaFileName)
    b.setContent(content)
    b.build()
  }
}
