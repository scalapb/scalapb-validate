import Settings.stdSettings
import scalapb.compiler.Version.scalapbVersion

val Scala213 = "2.13.7"

val Scala212 = "2.12.15"

val Scala3 = "3.1.0"

publish / skip := true

sonatypeProfileName := "com.thesamet"

inThisBuild(
  List(
    organization := "com.thesamet.scalapb",
    homepage := Some(url("https://github.com/scalapb/scalapb-validate")),
    licenses := List(
      "Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")
    ),
    developers := List(
      Developer(
        "thesamet",
        "Nadav Samet",
        "thesamet@gmail.com",
        url("https://www.thesamet.com")
      )
    ),
    PB.protocVersion := "3.15.6"
  )
)

val pgvVersion = "0.6.1"
val munitSettings = Seq(
  libraryDependencies += "org.scalameta" %% "munit" % "0.7.29" % Test,
  testFrameworks += new TestFramework("munit.Framework")
)

lazy val core = projectMatrix
  .in(file("core"))
  .defaultAxes()
  .settings(stdSettings)
  .settings(munitSettings)
  .settings(
    name := "scalapb-validate-core",
    libraryDependencies ++= Seq(
      "com.thesamet.scalapb.common-protos" %% "pgv-proto-scalapb_0.11" % (pgvVersion + "-0"),
      "com.thesamet.scalapb.common-protos" %% "pgv-proto-scalapb_0.11" % (pgvVersion + "-0") % "protobuf",
      "com.thesamet.scalapb" %% "scalapb-runtime" % scalapbVersion % "protobuf"
    ),
    Compile / PB.targets := Seq(
      PB.gens.java -> (Compile / sourceManaged).value / "scalapb",
      scalapb.gen() -> (Compile / sourceManaged).value / "scalapb"
    ),
    Compile / packageBin / packageOptions += {
      Package.ManifestAttributes(
        "ScalaPB-Options-Proto" ->
          "scalapb/validate-options.proto"
      )
    },
    compileOrder := CompileOrder.JavaThenScala
  )
  .jvmPlatform(scalaVersions = Seq(Scala212, Scala213, Scala3))

lazy val cats = projectMatrix
  .in(file("cats"))
  .dependsOn(core)
  .defaultAxes()
  .settings(stdSettings)
  .settings(munitSettings)
  .settings(
    name := "scalapb-validate-cats",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % "2.7.0" % "provided",
      "com.thesamet.scalapb" %% "scalapb-runtime" % scalapbVersion % "provided"
    )
  )
  .jvmPlatform(scalaVersions = Seq(Scala212, Scala213, Scala3))

lazy val codeGen = projectMatrix
  .in(file("code-gen"))
  .defaultAxes()
  .enablePlugins(BuildInfoPlugin)
  .settings(stdSettings)
  .settings(munitSettings)
  .settings(
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "scalapb.validate.compiler",
    name := "scalapb-validate-codegen",
    libraryDependencies ++= Seq(
      "com.thesamet.scalapb" %% "compilerplugin" % scalapbVersion,
      // scalapb-runtime does not gent automatically added since we do not have Scala gen,
      // and we want to make sure that a possibly older runtime (with different scalapb.proto)
      // gets in through the common-protos dependency.
      "com.thesamet.scalapb" %% "scalapb-runtime" % scalapbVersion,
      "com.thesamet.scalapb" %% "scalapb-runtime" % scalapbVersion % "protobuf",
      "com.thesamet.scalapb.common-protos" %% "pgv-proto-scalapb_0.11" % (pgvVersion + "-0") % "protobuf",
      "com.thesamet.scalapb.common-protos" %% "pgv-proto-scalapb_0.11" % (pgvVersion + "-0")
    ),
    Compile / PB.protoSources += core.base / "src" / "main" / "protobuf",
    Compile / PB.targets := Seq(
      PB.gens.java -> (Compile / sourceManaged).value / "scalapb"
    ),
    compileOrder := CompileOrder.JavaThenScala
  )
  .jvmPlatform(scalaVersions = Seq(Scala212, Scala213, Scala3))

lazy val codeGenJVM212 = codeGen.jvm(Scala212)

lazy val protocGenScalaPbValidate =
  protocGenProject("protoc-gen-scalapb-validate", codeGenJVM212)
    .settings(
      Compile / mainClass := Some("scalapb.validate.compiler.CodeGenerator")
    )

lazy val e2e = projectMatrix
  .in(file("e2e"))
  .defaultAxes()
  .dependsOn(core, cats)
  .enablePlugins(LocalCodeGenPlugin)
  .settings(stdSettings)
  .settings(munitSettings)
  .settings(
    publish / skip := true,
    crossScalaVersions := Seq(Scala212, Scala213),
    codeGenClasspath := (codeGenJVM212 / Compile / fullClasspath).value,
    libraryDependencies ++= Seq(
      "com.thesamet.scalapb" %% "scalapb-json4s" % "0.12.0",
      "org.typelevel" %% "cats-core" % "2.7.0",
      "io.undertow" % "undertow-core" % "2.2.14.Final",
      "eu.timepit" %% "refined" % "0.9.29",
      "io.envoyproxy.protoc-gen-validate" % "pgv-java-stub" % pgvVersion % "protobuf"
    ),
    TestProtosGenerator.generateAllTypesProtoSettings,
    Compile / PB.protoSources += (Compile / sourceManaged).value / "protobuf",
    Compile / PB.protoSources ++= (if (scalaVersion.value.startsWith("2."))
                                     Seq(
                                       (Compile / sourceDirectory).value / "protobuf-scala2"
                                     )
                                   else Seq.empty),
    Compile / PB.targets := Seq(
      genModule("scalapb.validate.compiler.ValidatePreprocessor$") ->
        (Compile / sourceManaged).value / "scalapb",
      scalapb.gen(grpc = true) -> (Compile / sourceManaged).value / "scalapb",
      genModule("scalapb.validate.compiler.CodeGenerator$") ->
        (Compile / sourceManaged).value / "scalapb"
    )
  )
  .jvmPlatform(scalaVersions = Seq(Scala212, Scala213, Scala3))
