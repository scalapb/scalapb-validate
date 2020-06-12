package scalapb.validate.compiler

import com.google.protobuf.Descriptors.{Descriptor, FieldDescriptor}
import com.google.protobuf.ExtensionRegistry
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse
import io.envoyproxy.pgv.validate.Validate
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
      .add(
        "import scalapb.validate.NumericValidator.{durationOrdering, timestampOrdering}"
      )
      .call(printObject)
      .result()
  }

  sealed trait RenderedResult

  case class SingularResult(fd: FieldDescriptor, accessor: String, line: Rule)
      extends RenderedResult

  case class OptionalResult(
      fd: FieldDescriptor,
      accessor: String,
      lines: Seq[Rule]
  ) extends RenderedResult

  case class RepeatedResult(
      fd: FieldDescriptor,
      accessor: String,
      lines: Seq[Rule]
  ) extends RenderedResult

  def renderedRulesForField(fd: FieldDescriptor): Seq[RenderedResult] = {
    val rulesProto =
      FieldRules.fromJavaProto(fd.getOptions.getExtension(Validate.rules))
    val accessor =
      if (!fd.isInOneof) s"input.${fd.scalaName.asSymbol}"
      else
        s"input.${fd.getContainingOneof.scalaName.nameSymbol}.${fd.scalaName}"

    val rules = RulesGen.rulesSingle(rulesProto)

    val maybeOpt =
      if ((fd.isInOneof || fd.supportsPresence) && rules.nonEmpty)
        Seq(
          OptionalResult(
            fd,
            accessor,
            rules
          )
        )
      else Seq.empty

    val maybeRepeated = if (fd.isRepeated() && !fd.isMapField()) {
      val itemRules = RulesGen.rulesSingle(rulesProto.getRepeated.getItems)

      val messageRules = Rule.ifSet(
        fd.isMessage &&
          !rulesProto.getRepeated.getItems.getMessage.getSkip &&
          !fd.getMessageType.getFullName.startsWith("google.protobuf")
      )(MessageValidateRule(validatorName(fd.getMessageType()).fullName))

      val allRules = itemRules ++ messageRules

      if (allRules.nonEmpty)
        Seq(
          RepeatedResult(
            fd,
            accessor,
            allRules
          )
        )
      else Seq.empty
    } else Seq.empty

    val messageRules = if (fd.isMessage) {
      val maybeRequired: Option[SingularResult] = None
      /*
      val maybeRequired =
        Rule.ifSet(fd.supportsPresence && rulesProto.getMessage.getRequired)(
          SingularResult(
            fd,
            s"""scalapb.validate.RequiredValidation("${fd
            .getName()}", ${accessor})""")
        )
       */

      val maybeNested = Rule.ifSet(
        fd.supportsPresence && !rulesProto.getMessage.getSkip &&
          !fd.getMessageType.getFullName.startsWith("google.protobuf")
      )(
        OptionalResult(
          fd,
          accessor,
          Seq(Rule.messageValidate(validatorName(fd.getMessageType).fullName))
        )
      )

      maybeRequired ++ maybeNested
    } else Seq.empty

    val maybeRequired = Rule.ifSet(RulesGen.isRequired(rulesProto))(
      SingularResult(fd, accessor, RequiredRulesGen.requiredRule)
    )

    val maybeSingular =
      if (!fd.supportsPresence && !fd.isInOneof && rules.nonEmpty)
        rules.map(r => SingularResult(fd, accessor, r))
      else Seq.empty

    maybeSingular ++ maybeOpt ++ maybeRepeated ++ messageRules ++ maybeRequired
  }

  def formattedRulesForField(
      fd: FieldDescriptor
  ): Seq[Seq[String]] =
    renderedRulesForField(fd).map {
      case SingularResult(fd, accessor, line) =>
        Seq(line.render(fd, accessor))
      case OptionalResult(fd, accessor, lines0) =>
        val lines = lines0.map(_.render(fd, "_value"))
        Seq(s"scalapb.validate.Result.optional($accessor) { _value =>") ++
          lines.dropRight(1).map(l => l + " &&") ++
          Seq(lines.last, "}")
      case RepeatedResult(fd, accessor, lines0) =>
        val lines = lines0.map(_.render(fd, "_value"))
        Seq(s"scalapb.validate.Result.repeated($accessor) { _value =>") ++
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
