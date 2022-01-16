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
import scalapb.compiler.EnclosingType

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
            if (!message.getOptions.getExtension(Validate.ignored))
          } yield new MessagePrinter(implicits, message).result()).flatten,
          Set(CodeGeneratorResponse.Feature.FEATURE_PROTO3_OPTIONAL)
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
  import DescriptorImplicits.AsSymbolExtension
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

  val fieldRules: Seq[(FieldDescriptor, Rule)] =
    message
      .getFields()
      .asScala
      .toSeq
      .flatMap { fd =>
        ruleForField(fd) match {
          case Some(rule) => Seq((fd, rule))
          case None       => Seq.empty
        }
      }

  def toBase(fd: FieldDescriptor, e: String): String =
    if (
      fd.customSingleScalaTypeName.isDefined && (!fd.isMessage || !fd
        .getMessageType()
        .getFullName
        .startsWith("google.protobuf."))
    ) {
      val tm = fd.typeMapper.fullName
      if (fd.isRepeated() && !fd.isMapField())
        s"${fd.collection.iterator(e, EnclosingType.None)}.map($tm.toBase)"
      else if (fd.isMapField()) e
      else if (fd.supportsPresence) s"$e.map($tm.toBase)"
      else s"$tm.toBase($e)"
    } else {
      if (fd.isRepeated && !fd.isMapField)
        fd.collection.iterator(e, EnclosingType.None)
      else e
    }

  def printValidate(fp: FunctionalPrinter): FunctionalPrinter = {
    val fieldRulesGroup = fieldRules.map { case (fd, rule) =>
      // Accessor that has the elements type mapped to base type.
      val accessor =
        if (fd.isRepeated())
          s"input.${fd.scalaName.asSymbol}"
        else if (!fd.isInOneof) toBase(fd, s"input.${fd.scalaName.asSymbol}")
        else
          s"input.${fd.getContainingOneof.scalaName.nameSymbol}.${fd.scalaName.asSymbol}"

      rule.render(fd, accessor)(FunctionalPrinter()).content
    }

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
  }

  def printObject(fp: FunctionalPrinter): FunctionalPrinter =
    fp.add(
      s"object ${objectName.name} extends $Validator[${message.scalaType.fullName}] {"
    ).indented(
      _.seq(fieldRules.flatMap(_._2.preamble))
        .call(printValidate)
        .print(
          message.getNestedTypes.asScala
            .filterNot(
              _.getOptions().getExtension(Validate.ignored).booleanValue()
            )
        )((fp, fd) => new MessagePrinter(implicits, fd).printObject(fp))
    ).add("}")

  def content: String = {
    val fp = new FunctionalPrinter()
    val imports = fieldRules.flatMap(_._2.imports).distinct
    fp.add(s"package ${message.getFile.scalaPackage.fullName}", "")
      .add()
      .seq(imports.map(i => s"import $i"))
      .call(printObject)
      .result()
  }

  def repeatedRules(
      fd: FieldDescriptor,
      rulesProto: FieldRules,
      inputTransform: String => String
  ): Seq[Rule] = {
    val itemRules = RulesGen.rulesSingle(fd, rulesProto, implicits)

    val messageRules = Rule.ifSet(
      fd.isMessage &&
        !rulesProto.getMessage.getSkip &&
        !fd.getMessageType.getFullName.startsWith("google.protobuf")
    )(Rule.messageValidate(validatorName(fd.getMessageType()).fullName))

    val allRules = itemRules ++ messageRules

    if (allRules.nonEmpty)
      Seq(
        RepeatedFieldRule(
          allRules,
          inputTransform
        )
      )
    else Seq.empty
  }

  def ruleForField(fd: FieldDescriptor): Option[Rule] = {
    val rulesProto =
      FieldRules.fromJavaProto(fd.getOptions.getExtension(Validate.rules))

    val rules = innerRulesForField(fd)
    val maybeEmptyRule =
      IgnoreEmptyRulesGen.ignoreEmptyRule(fd, rulesProto, implicits)

    (rules, maybeEmptyRule) match {
      case (Nil, _)                     => None
      case (rule :: Nil, None)          => Some(rule)
      case (rule :: Nil, Some(isEmpty)) => Some(IgnoreEmptyRule(isEmpty, rule))
      case (rules, None)                => Some(CombineFieldRules(rules, "&&"))
      case (rules, Some(isEmpty)) =>
        Some(IgnoreEmptyRule(isEmpty, CombineFieldRules(rules, "&&")))
    }
  }

  def innerRulesForField(fd: FieldDescriptor): Seq[Rule] = {
    val rulesProto =
      FieldRules.fromJavaProto(fd.getOptions.getExtension(Validate.rules))

    val rules = RulesGen.rulesSingle(fd, rulesProto, implicits)

    val maybeOpt =
      if ((fd.isInOneof || fd.supportsPresence) && rules.nonEmpty)
        Seq(
          OptionalFieldRule(
            rules
          )
        )
      else Seq.empty

    val maybeSingular =
      if (!fd.supportsPresence && !fd.isInOneof && rules.nonEmpty)
        rules
      else Seq.empty

    val maybeRepeated: Seq[Rule] =
      if (fd.isRepeated() && !fd.isMapField())
        repeatedRules(
          fd,
          rulesProto.getRepeated.getItems,
          e => toBase(fd, e)
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
          accessor =>
            fd.collection
              .iterator(accessor, EnclosingType.None) + accessKeyOrValue(1)
        ) ++
          repeatedRules(
            fd.getMessageType().findFieldByNumber(2),
            rulesProto.getMap.getValues,
            accessor =>
              fd.collection
                .iterator(accessor, EnclosingType.None) + accessKeyOrValue(2)
          )
      } else Seq.empty

    val messageRules = if (fd.isMessage) {
      val maybeRequired: Option[Rule] = Rule.ifSet(
        !fd.supportsPresence && !fd.isRepeated && !fd.isInOneof && !rulesProto.getMessage.getSkip &&
          !fd.getMessageType.getFullName.startsWith("google.protobuf") &&
          !fd.getMessageType.getFile.scalaOptions
            .getExtension(scalapb.validate.Validate.file)
            .getSkip
      )(
        Rule.messageValidate(validatorName(fd.getMessageType).fullName)
      )

      val maybeNested = Rule.ifSet(
        (fd.supportsPresence || fd.isInOneof) && !rulesProto.getMessage.getSkip &&
          !fd.getMessageType.getFullName.startsWith("google.protobuf") &&
          !fd.getMessageType.getFile.scalaOptions
            .getExtension(scalapb.validate.Validate.file)
            .getSkip
      )(
        OptionalFieldRule(
          Seq(Rule.messageValidate(validatorName(fd.getMessageType).fullName))
        )
      )

      maybeRequired ++ maybeNested
    } else Seq.empty

    val maybeRequired =
      Rule.ifSet(RulesGen.isRequired(rulesProto) && !fd.noBoxRequired)(
        RequiredRulesGen.requiredRule
      )

    maybeSingular ++ maybeOpt ++ maybeRepeated ++ messageRules ++ maybeRequired
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

    if (!fileOpts.getSkip)
      (Seq(validationFile) ++ (if (insertValidation) Seq(companionInsertion)
                               else Nil) ++ (if (insertToConstructor)
                                               Seq(constructorInsertion)
                                             else Nil))
    else Seq.empty
  }
}
