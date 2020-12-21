package scalapb.validate.compiler

import com.google.protobuf.ExtensionRegistry
import protocgen.{CodeGenApp, CodeGenRequest, CodeGenResponse}
import scalapb.options.Scalapb
import io.envoyproxy.pgv.validate.Validate
import protocbridge.Artifact
import com.google.protobuf.Descriptors.FileDescriptor
import scala.jdk.CollectionConverters._
import com.google.protobuf.Descriptors.Descriptor
import com.google.protobuf.Descriptors.FieldDescriptor
import scalapb.validate.Validate.FieldTransformation.MatchType
import scalapb.options.Scalapb.Collection
import com.google.protobuf.Message
import scalapb.options.Scalapb.PreprocesserOutput
import scalapb.options.Scalapb.ScalaPbOptions
import java.nio.file.Files
import scalapb.validate.Validate.FieldTransformation
import com.google.protobuf.TextFormat
import scalapb.options.Scalapb.ScalaPbOptions.AuxFieldOptions

object ValidatePreprocessor extends CodeGenApp {
  override def registerExtensions(registry: ExtensionRegistry): Unit = {
    Scalapb.registerAllExtensions(registry)
    Validate.registerAllExtensions(registry)
    scalapb.validate.Validate.registerAllExtensions(registry)
  }

  override def suggestedDependencies: Seq[Artifact] = Seq.empty

  val NonEmptySet = Collection
    .newBuilder()
    .setType("_root_.cats.data.NonEmptySet")
    .setAdapter("_root_.scalapb.validate.cats.NonEmptySetAdapter")
    .setNonEmpty(true)
    .build()

  val NonEmptyList = Collection
    .newBuilder()
    .setType("_root_.cats.data.NonEmptyList")
    .setAdapter("_root_.scalapb.validate.cats.NonEmptyListAdapter")
    .setNonEmpty(true)
    .build()

  val NonEmptyMap = Collection
    .newBuilder()
    .setType("_root_.cats.data.NonEmptyMap")
    .setAdapter("_root_.scalapb.validate.cats.NonEmptyMapAdapter")
    .setNonEmpty(true)
    .build()

  def process(request: CodeGenRequest): CodeGenResponse = {
    val secondaryResult = new ProcessRequest(request).result()
    val b = com.google.protobuf.Any
      .pack(secondaryResult)
      .toByteArray()
    val secondaryOutputFile = scalapb.compiler.SecondaryOutputProvider
      .secondaryOutputDir(
        request.asProto
      )
      .getOrElse(
        throw new RuntimeException(
          "Secondary output dir not provided. The most likely reason is that you are using an old version of sbt-protoc/protocbridge that does not provide this information. If you are invoking this plugin directly, SECONDARY_OUTPUT_DIR must be provided as an environment variable."
        )
      )
      .toPath
      .resolve(ValidatePreprocessor.PREPROCESSOR_NAME)

    Files.write(secondaryOutputFile, b)

    CodeGenResponse.succeed(Nil)
  }

  val PREPROCESSOR_NAME = "scalapb-validate-preprocessor"
}

class ProcessRequest(req: CodeGenRequest) {

  val cache = PackageOptionsCache.from(req.allProtos)

  def processFile(file: FileDescriptor): Option[ScalaPbOptions] = {
    val b = Scalapb.ScalaPbOptions.newBuilder()

    file.getMessageTypes().asScala.foreach(processMessage(b, _))
    val result = b.build()

    if (result.getSerializedSize() != 0) Some(result) else None
  }

  def result(): PreprocesserOutput = {
    val outs = req.allProtos.map(f => (f.getName(), processFile(f))).collect {
      case (name, Some(out)) => name -> out
    }

    scalapb.options.Scalapb.PreprocesserOutput
      .newBuilder()
      .putAllOptionsByFile(outs.toMap.asJava)
      .build()
  }

  def processMessage(
      builder: ScalaPbOptions.Builder,
      message: Descriptor
  ): Unit = {
    message.getNestedTypes().asScala.foreach(processMessage(builder, _))
    message.getFields().asScala.foreach(processField(builder, _))
  }

  def matches[T <: Message](msg: T, pattern: T, matchType: MatchType): Boolean =
    matchType match {
      case MatchType.CONTAINS => msg.toBuilder().mergeFrom(pattern).build == msg
      case MatchType.EXACT    => msg == pattern
    }

  // Given a FieldTranformation and a field (with possibly pgv rules), determine if the
  // transformation applies using the `when` condition, and if so returns Some(auxFieldOptios)
  // that describe an update to this field ScalaPB options.
  def auxFieldOptionsForTransformation(
      t: FieldTransformation,
      field: FieldDescriptor
  ): Option[AuxFieldOptions] = {
    val fieldRule = field.getOptions().getExtension(Validate.rules)
    if (
      matches(fieldRule, t.getWhen(), t.getMatchType()) ||
      matches(
        fieldRule.getRepeated().getItems(),
        t.getWhen(),
        t.getMatchType()
      )
    ) {
      Some(
        AuxFieldOptions.newBuilder
          .setTarget(field.getFullName())
          .setOptions(t.getSet())
          .build
      )
    } else {
      val matchesMapKey =
        matches(fieldRule.getMap().getKeys(), t.getWhen(), t.getMatchType())
      val matchesMapValue = matches(
        fieldRule.getMap().getValues(),
        t.getWhen(),
        t.getMatchType()
      )
      if (matchesMapKey || matchesMapValue) {
        val b = AuxFieldOptions
          .newBuilder()
          .setTarget(field.getFullName())
          .setOptions {
            // For convenience we clean up `type` and apply it to map_key and/or map_value
            // depending on where the match happened.
            val optBuilder = t.getSet().toBuilder()
            optBuilder.clearType
            if (matchesMapKey) optBuilder.setKeyType(t.getSet.getType)
            if (matchesMapValue) optBuilder.setValueType(t.getSet.getType)
            optBuilder.build()
          }
        Some(b.build)
      } else None
    }
  }

  def processField(
      b: ScalaPbOptions.Builder,
      field: FieldDescriptor
  ): Unit = {
    val packageOptions = cache.get(field.getFile.getPackage())
    b.addAllAuxFieldOptions(
      (for {
        transform <- packageOptions.getFieldTransformationsList().asScala
        auxOptions <- auxFieldOptionsForTransformation(transform, field)
      } yield auxOptions).asJava
    )
    val catsRules =
      if (packageOptions.getCatsTransforms)
        CatsRules
          .flatMap(auxFieldOptionsForTransformation(_, field))
          .headOption
      else None

    val setRules =
      if (packageOptions.getUniqueToSet())
        SetRules.flatMap(auxFieldOptionsForTransformation(_, field)).headOption
      else None

    catsRules.orElse(setRules).foreach {
      b.addAuxFieldOptions(_)
    }
    ()
  }

  def fieldTransformation(s: String): FieldTransformation = {
    val er = ExtensionRegistry.newInstance()
    ValidatePreprocessor.registerExtensions(er)
    TextFormat.parse(s, er, classOf[FieldTransformation])
  }

  val SetRules = Seq(
    fieldTransformation("""when: {
          repeated: {unique: true}
        }
        set: {
          collection_type: "_root_.scala.collection.immutable.Set"
        }""".stripMargin)
  )

  val CatsRules =
    Seq(
      fieldTransformation("""when: {
             repeated: {unique: true, min_items: 1}
           }
           set: {
             collection: {
               type: "_root_.cats.data.NonEmptySet"
               adapter: "_root_.scalapb.validate.cats.NonEmptySetAdapter"
               non_empty: true
             }
           }"""),
      fieldTransformation("""when: {
             repeated: {min_items: 1}
           }
           set: {
             collection: {
               type: "_root_.cats.data.NonEmptyList"
               adapter: "_root_.scalapb.validate.cats.NonEmptyListAdapter"
               non_empty: true
             }
           }"""),
      fieldTransformation("""when: {
             map: {min_pairs: 1}
           }
           set: {
             collection: {
               type: "_root_.cats.data.NonEmptyMap"
               adapter: "_root_.scalapb.validate.cats.NonEmptyMapAdapter"
               non_empty: true
             }
           }""")
    )
}
