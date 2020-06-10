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
    homepage := Some(url("https://github.com/scalameta/sbt-scalafmt")),
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
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "scalapb.validate.compiler",
    name := "scalapb-validate-codegen",
    libraryDependencies ++= Seq(
      "com.thesamet.scalapb" %% "compilerplugin" % scalapb.compiler.Version.scalapbVersion,
      "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion,
      "com.thesamet.scalapb.common-protos" %% "pgv-proto-scalapb_0.10" % (pgvVersion + "-0")
    )
  )

def projDef(name: String, shebang: Boolean) =
  sbt
    .Project(name, new File(name))
    .enablePlugins(AssemblyPlugin)
    .dependsOn(codeGen)
    .settings(stdSettings)
    .settings(
      assemblyOption in assembly := (assemblyOption in assembly).value.copy(
        prependShellScript = Some(
          sbtassembly.AssemblyPlugin.defaultUniversalScript(shebang = shebang)
        )
      ),
      skip in publish := true,
      Compile / mainClass := Some("scalapb.validate.compiler.CodeGenerator")
    )

lazy val protocGenScalaPbValidateUnix =
  projDef("protoc-gen-scalapb-validate-unix", shebang = true)

lazy val protocGenScalaPbValidateWindows =
  projDef("protoc-gen-scalapb-validate-windows", shebang = false)

lazy val protocGenScalaPbValidate = project
  .settings(
    crossScalaVersions := List(Scala213),
    name := "protoc-gen-scalapb-validate",
    publishArtifact in (Compile, packageDoc) := false,
    publishArtifact in (Compile, packageSrc) := false,
    crossPaths := false,
    addArtifact(
      Artifact("protoc-gen-scalapb-validate", "jar", "sh", "unix"),
      assembly in (protocGenScalaPbValidateUnix, Compile)
    ),
    addArtifact(
      Artifact("protoc-gen-scalapb-validate", "jar", "bat", "windows"),
      assembly in (protocGenScalaPbValidateWindows, Compile)
    ),
    autoScalaLibrary := false
  )

lazy val e2e = project
  .in(file("e2e"))
  .dependsOn(core)
  .settings(stdSettings)
  .settings(
    skip in publish := true,
    libraryDependencies ++= Seq(
      "io.envoyproxy.protoc-gen-validate" % "pgv-java-stub" % pgvVersion % "protobuf",
      "com.lihaoyi" %% "cask" % "0.6.5"
    ),
    Compile / PB.generate := ((Compile / PB.generate) dependsOn (protocGenScalaPbValidateUnix / Compile / assembly)).value,
    PB.targets in Compile := Seq(
      scalapb.gen(grpc = true) -> (sourceManaged in Compile).value,
      (
        PB.gens.plugin(
          "validate",
          (protocGenScalaPbValidateUnix / assembly / target).value / "protoc-gen-scalapb-validate-unix-assembly-" + version.value + ".jar"
        ),
        Seq()
      ) -> (Compile / sourceManaged).value
    ),
    Compile / PB.recompile := true // always regenerate protos, not cache
  )
