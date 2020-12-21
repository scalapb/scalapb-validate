package scalapb.validate.compiler

import com.google.protobuf.Descriptors.{Descriptor, FieldDescriptor}
import com.google.protobuf.ExtensionRegistry
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse
import io.envoyproxy.pgv.validate.Validate
import io.envoyproxy.pgv.validate.validate.FieldRules
import protocbridge.Artifact
import protocgen.{CodeGenApp, CodeGenRequest, CodeGenResponse}
import scalapb.compiler.{
  DescriptorImplicits,
  FunctionalPrinter,
  ProtobufGenerator
}
import scalapb.options.Scalapb
import scalapb.validate.compat.JavaConverters._
import com.google.protobuf.Descriptors.OneofDescriptor

object CodeGenerator extends CodeGenApp {
  override def registerExtensions(registry: ExtensionRegistry): Unit = {
    Scalapb.registerAllExtensions(registry)
    Validate.registerAllExtensions(registry)
    scalapb.validate.Validate.registerAllExtensions(registry)
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
          DescriptorImplicits.fromCodeGenRequest(params, request)
        CodeGenResponse.succeed(
          (for {
            file <- request.filesToGenerate
            message <- file.getMessageTypes().asScala
          } yield new MessagePrinter(implicits, message).result()).flatten
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

  val fieldRules: Seq[RenderedResult] =
    message.getFields().asScala.toSeq.flatMap(renderedRulesForField(_))

  def printValidate(fp: FunctionalPrinter): FunctionalPrinter = {
    val fieldRulesGroup = fieldRules.map(formatRenderedResult(_))

    val ruleGroups =
      fieldRulesGroup ++
        message.getOneofs.asScala.toSeq.flatMap(formattedRulesForOneofs)
    val isDisabled = message.getOptions().getExtension(Validate.disabled)
    fp.add(
      s"def validate(input: ${message.scalaType.fullName}): $Result ="
    ).when(!isDisabled)(
      _.indented(
        _.addGroupsWithDelimiter(" &&")(ruleGroups)
      )
    ).when(ruleGroups.isEmpty || isDisabled)(
      _.add("  scalapb.validate.Success")
    ).add("")
      .print(message.getNestedTypes.asScala)((fp, fd) =>
        new MessagePrinter(implicits, fd).printObject(fp)
      )
  }

  def printObject(fp: FunctionalPrinter): FunctionalPrinter =
    fp.add(
      s"object ${objectName.name} extends $Validator[${message.scalaType.fullName}] {"
    ).indented(
      _.seq(fieldRules.flatMap(_.preambles))
        .call(printValidate)
    ).add("}")

  def content: String = {
    val fp = new FunctionalPrinter()
    val imports = fieldRules.flatMap(_.imports).distinct
    fp.add(s"package ${message.getFile.scalaPackage.fullName}", "")
      .add()
      .seq(imports.map(i => s"import $i"))
      .call(printObject)
      .result()
  }

  sealed trait RenderedResult {
    def imports: Seq[String]

    def preambles: Seq[String]
  }

  case class SingularResult(fd: FieldDescriptor, accessor: String, line: Rule)
      extends RenderedResult {
    def imports = line.imports

    def preambles = line.preamble
  }

  case class OptionalResult(
      fd: FieldDescriptor,
      accessor: String,
      lines: Seq[Rule]
  ) extends RenderedResult {
    def imports = lines.flatMap(_.imports)
    def preambles = lines.flatMap(_.preamble)
  }

  case class RepeatedResult(
      fd: FieldDescriptor,
      accessor: String,
      lines: Seq[Rule]
  ) extends RenderedResult {
    def imports = lines.flatMap(_.imports)
    def preambles = lines.flatMap(_.preamble)
  }

  def repeatedRules(
      fd: FieldDescriptor,
      rulesProto: FieldRules,
      accessor: String
  ): Seq[RenderedResult] = {
    val itemRules = RulesGen.rulesSingle(fd, rulesProto, implicits)

    val messageRules = Rule.ifSet(
      fd.isMessage &&
        !rulesProto.getMessage.getSkip &&
        !fd.getMessageType.getFullName.startsWith("google.protobuf")
    )(Rule.messageValidate(validatorName(fd.getMessageType()).fullName))

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
  }

  def renderedRulesForField(fd: FieldDescriptor): Seq[RenderedResult] = {
    def toBase(e: String): String =
      if (
        fd.customSingleScalaTypeName.isDefined && (!fd.isMessage || !fd
          .getMessageType()
          .getFullName
          .startsWith("google.protobuf."))
      ) {
        val tm = fd.typeMapper.fullName
        if (fd.isRepeated() && !fd.isMapField())
          s"${fd.collection.iterator(e)}.map($tm.toBase)"
        else if (fd.isMapField()) e
        else if (fd.supportsPresence) s"$e.map($tm.toBase)"
        else s"${tm}.toBase($e)"
      } else {
        if (fd.isRepeated && !fd.isMapField) fd.collection.iterator(e)
        else e
      }

    val rulesProto =
      FieldRules.fromJavaProto(fd.getOptions.getExtension(Validate.rules))

    // Accessor that has the elements type mapped to base type.
    val accessor =
      if (!fd.isInOneof) toBase(s"input.${fd.scalaName.asSymbol}")
      else
        s"input.${fd.getContainingOneof.scalaName.nameSymbol}.${fd.scalaName}"

    // For repeated fields, this gives the collection itself. This is possibly
    // a custom collection type with custom type items.
    val collectionAccessor = s"input.${fd.scalaName.asSymbol}"

    val rules = RulesGen.rulesSingle(fd, rulesProto, implicits)

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

    val maybeRepeated: Seq[RenderedResult] =
      if (fd.isRepeated() && !fd.isMapField())
        repeatedRules(
          fd,
          rulesProto.getRepeated.getItems,
          accessor
        )
      else if (fd.isRepeated() && fd.isMapField()) {
        def accessKeyOrValue(index: Int) = {
          val kvDesc = fd.getMessageType().findFieldByNumber(index)
          if (kvDesc.customSingleScalaTypeName.isEmpty) s".map(_._$index)"
          else {
            val tm = kvDesc.typeMapper.fullName
            s".map(__e => $tm.toBase(__e._$index))"
          }
        }
        repeatedRules(
          fd.getMessageType().findFieldByNumber(1),
          rulesProto.getMap.getKeys,
          fd.collection.iterator(accessor) + accessKeyOrValue(1)
        ) ++
          repeatedRules(
            fd.getMessageType().findFieldByNumber(2),
            rulesProto.getMap.getValues,
            fd.collection.iterator(accessor) + accessKeyOrValue(2)
          )
      } else Seq.empty

    val messageRules = if (fd.isMessage) {
      val maybeRequired: Option[SingularResult] = Rule.ifSet(
        !fd.supportsPresence && !fd.isRepeated && !fd.isInOneof && !rulesProto.getMessage.getSkip &&
          !fd.getMessageType.getFullName.startsWith("google.protobuf")
      )(
        SingularResult(
          fd,
          accessor,
          Rule.messageValidate(validatorName(fd.getMessageType).fullName)
        )
      )

      val maybeNested = Rule.ifSet(
        (fd.supportsPresence || fd.isInOneof) && !rulesProto.getMessage.getSkip &&
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
        rules.map(r =>
          SingularResult(
            fd,
            if (fd.isRepeated) collectionAccessor else accessor,
            r
          )
        )
      else Seq.empty

    maybeSingular ++ maybeOpt ++ maybeRepeated ++ messageRules ++ maybeRequired
  }

  def formatRenderedResult(
      result: RenderedResult
  ): Seq[String] =
    result match {
      case SingularResult(fd, accessor, line) =>
        Seq(line.render(fd, accessor))
      case OptionalResult(fd, accessor, lines0) =>
        val lines = lines0.map(_.render(fd, "_value"))
        Seq(s"scalapb.validate.Result.optional($accessor) { _value =>") ++
          lines.dropRight(1).map(l => "  " + l + " &&") ++
          Seq("  " + lines.last, "}")
      case RepeatedResult(fd, accessor, lines0) =>
        val lines = lines0.map(_.render(fd, "_value"))
        Seq(s"scalapb.validate.Result.repeated($accessor) { _value =>") ++
          lines.dropRight(1).map(l => "  " + l + " &&") ++
          Seq("  " + lines.last, "}")
    }

  def formattedRulesForOneofs(oneof: OneofDescriptor): Seq[Seq[String]] = {
    val isRequired = oneof.getOptions().getExtension(Validate.required)
    if (isRequired) Seq(Seq(s"""scalapb.validate.RequiredValidation("${oneof
      .getName()}", input.${oneof.scalaName.name})"""))
    else Seq.empty
  }

  def result(): Seq[CodeGeneratorResponse.File] = {
    val validationFile =
      CodeGeneratorResponse.File
        .newBuilder()
        .setName(scalaFileName)
        .setContent(content)
        .build()
    val companionInsertion = message.messageCompanionInsertionPoint.withContent(
      s"implicit val validator: $Validator[${message.scalaType.fullName}] = ${objectName.fullName}"
    )

    val constructorInsertion = message.messageClassInsertionPoint.withContent(
      s"_root_.scalapb.validate.Validator.assertValid(this)(${validatorName(message).fullName})"
    )
    val msgOpts = message.messageOptions
      .getExtension(scalapb.validate.Validate.message)
    val fileOpts = message
      .getFile()
      .scalaOptions
      .getExtension(scalapb.validate.Validate.file)

    val insertValidation =
      if (msgOpts.hasInsertValidatorInstance())
        msgOpts.getInsertValidatorInstance()
      else fileOpts.getInsertValidatorInstance()

    val insertToConstructor =
      if (msgOpts.hasValidateAtConstruction())
        msgOpts.getValidateAtConstruction()
      else fileOpts.getValidateAtConstruction()

    Seq(validationFile) ++ (if (insertValidation) Seq(companionInsertion)
                            else Nil) ++ (if (insertToConstructor)
                                            Seq(constructorInsertion)
                                          else Nil)

  }
}
