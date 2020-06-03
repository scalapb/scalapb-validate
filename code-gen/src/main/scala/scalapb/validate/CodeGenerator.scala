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
import io.envoyproxy.pgv.validate.Validate.FieldRules
import io.envoyproxy.pgv.validate.Validate.FieldRules.TypeCase
import io.envoyproxy.pgv.validate.Validate.StringRules
import io.envoyproxy.pgv.validate.Validate.Int32Rules
import io.envoyproxy.pgv.validate.Validate.MessageRules
import io.envoyproxy.pgv.validate.Validate.UInt64Rules

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

class MessagePrinter(
    implicits: DescriptorImplicits,
    message: Descriptor
) {
  import implicits._

  private val objectName =
    message.scalaType.sibling(message.scalaType.name + "Validator")

  def scalaFileName =
    message.getFile.scalaDirectory + "/" + objectName.name + ".scala"

  private val validator = "com.thesamet.scalapb.validate.Validator"

  def content: String = {
    val fp = new FunctionalPrinter()
    fp.add(s"package ${message.getFile.scalaPackage.fullName}", "")
      .add()
      .add(
        s"object ${objectName.name} extends $validator[${message.scalaType.fullName}] {"
      )
      .indented(
        _.add(
          s"def validate(input: ${message.scalaType.fullName}): Boolean = {"
        ).indented(
            _.print(message.getFields().asScala)(printField(_, _))
              .add("false")
          )
          .add("}")
      )
      .add("}")
      .result()
  }

  def printString(
      fp: FunctionalPrinter,
      fd: FieldDescriptor,
      rules: StringRules
  ): FunctionalPrinter =
    fp.add(s"/* Validation for ${fd.getName}: ${rules} */")

  def printInt32(
      fp: FunctionalPrinter,
      fd: FieldDescriptor,
      rules: Int32Rules
  ): FunctionalPrinter =
    fp.add(s"/* Validation for ${fd.getName}: ${rules} */")

  def printUInt64(
      fp: FunctionalPrinter,
      fd: FieldDescriptor,
      rules: UInt64Rules
  ): FunctionalPrinter =
    fp.add(s"/* Validation for ${fd.getName}: ${rules} */")

  def printMessage(
      fp: FunctionalPrinter,
      fd: FieldDescriptor,
      rules: MessageRules
  ): FunctionalPrinter =
    fp.add(s"/* Validation for ${fd.getName}: ${rules} */")

  def printField(
      fp: FunctionalPrinter,
      fd: FieldDescriptor
  ): FunctionalPrinter = {
    val rules = fd.getOptions().getExtension(Validate.rules)
    rules.getTypeCase() match {
      case TypeCase.STRING => printString(fp, fd, rules.getString)
      case TypeCase.INT32  => printInt32(fp, fd, rules.getInt32())
      case TypeCase.UINT64 => printUInt64(fp, fd, rules.getUint64())
      case TypeCase.TYPE_NOT_SET =>
        if (fd.isMessage && rules.hasMessage())
          printMessage(fp, fd, rules.getMessage())
        else fp
    }
  }

  def result(): CodeGeneratorResponse.File = {
    val b = CodeGeneratorResponse.File.newBuilder()
    b.setName(scalaFileName)
    b.setContent(content)
    b.build()
  }
}
