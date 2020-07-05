import Settings.stdSettings

val Scala213 = "2.13.2"

val Scala212 = "2.12.10"

ThisBuild / resolvers += Resolver.sonatypeRepo("snapshots")

ThisBuild / scalaVersion := Scala213

ThisBuild / crossScalaVersions := Seq(Scala212, Scala213)

skip in publish := true

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
    )
  )
)

val pgvVersion = "0.3.0"

lazy val core = project
  .in(file("core"))
  .settings(stdSettings)
  .settings(
    name := "scalapb-validate-core",
    libraryDependencies ++= Seq(
      "com.thesamet.scalapb.common-protos" %% "pgv-proto-scalapb_0.10" % (pgvVersion + "-0"),
      "com.thesamet.scalapb.common-protos" %% "pgv-proto-scalapb_0.10" % (pgvVersion + "-0") % "protobuf"
    )
  )

lazy val codeGen = project
  .in(file("code-gen"))
  .enablePlugins(BuildInfoPlugin)
  .settings(stdSettings)
  .settings(
    scalaVersion := Scala212,
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "scalapb.validate.compiler",
    name := "scalapb-validate-codegen",
    libraryDependencies ++= Seq(
      "com.thesamet.scalapb" %% "compilerplugin" % scalapb.compiler.Version.scalapbVersion,
      "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion,
      "com.thesamet.scalapb.common-protos" %% "pgv-proto-scalapb_0.10" % (pgvVersion + "-0")
    )
  )

lazy val protocGenScalaPbValidate =
  protocGenProject("protoc-gen-scalapb-validate", codeGen)
    .settings(
      Compile / mainClass := Some("scalapb.validate.compiler.CodeGenerator")
    )

lazy val e2e = project
  .in(file("e2e"))
  .dependsOn(core)
  .enablePlugins(LocalCodeGenPlugin)
  .settings(stdSettings)
  .settings(
    skip in publish := true,
    codeGenClasspath := (codeGen / Compile / fullClasspath).value,
    libraryDependencies ++= Seq(
      "io.undertow" % "undertow-core" % "2.1.3.Final",
      "io.envoyproxy.protoc-gen-validate" % "pgv-java-stub" % pgvVersion % "protobuf",
      "org.scalameta" %% "munit" % "0.7.9" % Test
    ),
    testFrameworks += new TestFramework("munit.Framework"),
    PB.targets in Compile := Seq(
      scalapb.gen(grpc = true) -> (sourceManaged in Compile).value / "scalapb",
      genModule("scalapb.validate.compiler.CodeGenerator$") ->
          (sourceManaged in Compile).value / "scalapb",
    )
  )
