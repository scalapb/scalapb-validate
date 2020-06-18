package scalapb.validate

import protocbridge.Artifact
import scalapb.GeneratorOption._
import scalapb.GeneratorOption
import protocbridge.SandboxedJvmGenerator

object gen {
  def apply(
      options: Set[GeneratorOption]
  ): (SandboxedJvmGenerator, Seq[String]) =
    (
      SandboxedJvmGenerator(
        "scala",
        Artifact(
          "com.thesamet.scalapb",
          "scalapb-validate-codegen_2.12",
          scalapb.validate.compiler.BuildInfo.version
        ),
        "scalapb.validate.compiler.CodeGenerator$",
        scalapb.validate.compiler.CodeGenerator.suggestedDependencies
      ),
      Seq(
        "flat_package" -> options(FlatPackage),
        "java_conversions" -> options(JavaConversions),
        "grpc" -> options(Grpc),
        "single_line_to_proto_string" -> options(SingleLineToProtoString),
        "ascii_format_to_string" -> options(AsciiFormatToString),
        "no_lenses" -> !options(Lenses),
        "retain_source_code_info" -> options(RetainSourceCodeInfo)
      ).collect { case (name, v) if v => name }
    )

  def apply(
      flatPackage: Boolean = false,
      javaConversions: Boolean = false,
      grpc: Boolean = true,
      singleLineToProtoString: Boolean = false,
      asciiFormatToString: Boolean = false,
      lenses: Boolean = true
  ): (SandboxedJvmGenerator, Seq[String]) = {
    val optionsBuilder = Set.newBuilder[GeneratorOption]
    if (flatPackage)
      optionsBuilder += FlatPackage
    if (javaConversions)
      optionsBuilder += JavaConversions
    if (grpc)
      optionsBuilder += Grpc
    if (singleLineToProtoString)
      optionsBuilder += SingleLineToProtoString
    if (asciiFormatToString)
      optionsBuilder += AsciiFormatToString
    if (lenses)
      optionsBuilder += Lenses
    gen(optionsBuilder.result())
  }
}
