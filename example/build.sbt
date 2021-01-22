name := "validate-example"

scalaVersion := "2.13.2"

ThisBuild / resolvers += Resolver.sonatypeRepo("snapshots")

ThisBuild / scalacOptions ++= Seq("-Xfatal-warnings", "-Xlint")

Compile / PB.targets := Seq(
  scalapb.gen() -> (Compile / sourceManaged).value / "scalapb",
  scalapb.validate.gen() -> (Compile / sourceManaged).value / "scalapb"
)

libraryDependencies ++= Seq(
  "com.thesamet.scalapb" %% "scalapb-validate-core" % scalapb.validate.compiler.BuildInfo.version % "protobuf"
)
