package scalapb.validate.compiler

import com.google.protobuf.ExtensionRegistry
import protocgen.{CodeGenApp, CodeGenRequest, CodeGenResponse}
import scalapb.options.Scalapb
import io.envoyproxy.pgv.validate.Validate
import protocbridge.Artifact
import com.google.protobuf.Descriptors.FileDescriptor
import scala.jdk.CollectionConverters._
import scalapb.options.Scalapb.Collection
import scalapb.options.Scalapb.PreprocessorOutput
import scalapb.options.Scalapb.ScalaPbOptions
import java.nio.file.Files
import scalapb.options.Scalapb.FieldTransformation
import com.google.protobuf.TextFormat
import io.envoyproxy.pgv.validate.Validate.FieldRules
import io.envoyproxy.pgv.validate.Validate.RepeatedRules
import io.envoyproxy.pgv.validate.Validate.MapRules

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

    val packageOptions = cache.get(file.getPackage())
    // Set rules done first, since Cats NonEmptySet is more specific and should override.
    val ts = (
      if (packageOptions.getUniqueToSet) SetRules else Nil
    ) ++ (
      if (packageOptions.getCatsTransforms) CatsRules else Nil
    )

    b.addAllFieldTransformations(ts.asJava)
    b.addAllFieldTransformations(
      file
        .getOptions()
        .getExtension(Scalapb.options)
        .getFieldTransformationsList()
        .asScala
        .flatMap(expandTransformation(_))
        .asJava
    )

    val result = b.build()

    if (result.getSerializedSize() != 0) Some(result) else None
  }

  def result(): PreprocessorOutput = {
    val outs = req.allProtos.map(f => (f.getName(), processFile(f))).collect {
      case (name, Some(out)) => name -> out
    }

    scalapb.options.Scalapb.PreprocessorOutput
      .newBuilder()
      .putAllOptionsByFile(outs.toMap.asJava)
      .build()
  }

  def expandTransformation(t: FieldTransformation): Seq[FieldTransformation] =
    if (!t.getWhen().hasExtension(Validate.rules)) Seq.empty
    else {
      val fieldRules = t.getWhen.getExtension(Validate.rules)
      if (fieldRules.hasRepeated() || fieldRules.hasMap()) Seq.empty
      else {
        val rep = t
          .toBuilder()
          .clearWhen()
        rep
          .getWhenBuilder()
          .setExtension(
            Validate.rules,
            FieldRules
              .newBuilder()
              .setRepeated(RepeatedRules.newBuilder.setItems(fieldRules))
              .build()
          )

        val mapKey = t
          .toBuilder()
          .clearWhen()
        if (t.getSet.hasType()) {
          mapKey
            .getSetBuilder()
            .clearType()
            .setKeyType(t.getSet.getType())
        }
        mapKey
          .getWhenBuilder()
          .setExtension(
            Validate.rules,
            FieldRules
              .newBuilder()
              .setMap(MapRules.newBuilder.setKeys(fieldRules).build())
              .build()
          )

        val mapValue = t
          .toBuilder()
          .clearWhen()
        mapValue
          .getSetBuilder()
          .clearType()
          .setKeyType(t.getSet.getType())
        if (t.getSet.hasType()) {
          mapValue
            .getSetBuilder()
            .clearType()
            .setValueType(t.getSet.getType())
        }
        mapValue
          .getWhenBuilder()
          .setExtension(
            Validate.rules,
            FieldRules
              .newBuilder()
              .setMap(MapRules.newBuilder.setValues(fieldRules).build())
              .build()
          )

        Seq(rep.build(), mapKey.build(), mapValue.build())
      }
    }

  def fieldTransformation(s: String): FieldTransformation = {
    val er = ExtensionRegistry.newInstance()
    ValidatePreprocessor.registerExtensions(er)
    TextFormat.parse(s, er, classOf[FieldTransformation])
  }

  val SetRules = Seq(
    fieldTransformation("""when: {
          [validate.rules] {
            repeated: {unique: true}
          }
        }
        set: {
          collection_type: "_root_.scala.collection.immutable.Set"
          collection: {
            type: "_root_.scala.collection.immutable.Set"
            adapter: "_root_.scalapb.validate.SetAdapter"
          }
          [scalapb.validate.field] {
            skip_unique_check: true
          }
        }""".stripMargin)
  )

  val CatsRules =
    Seq(
      fieldTransformation("""when: {
             [validate.rules] {
               repeated: {min_items: 1}
             }
           }
           set: {
             collection: {
               type: "_root_.cats.data.NonEmptyList"
               adapter: "_root_.scalapb.validate.cats.NonEmptyListAdapter"
               non_empty: true
             }
           }"""),
      fieldTransformation("""when: {
             [validate.rules] {
               repeated: {unique: true, min_items: 1}
             }
           }
           set: {
             collection: {
               type: "_root_.cats.data.NonEmptySet"
               adapter: "_root_.scalapb.validate.cats.NonEmptySetAdapter"
               non_empty: true
             }
             [scalapb.validate.field] {
               skip_unique_check: true
             }
           }"""),
      fieldTransformation("""when: {
             [validate.rules] {
               map: {min_pairs: 1}
             }
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
