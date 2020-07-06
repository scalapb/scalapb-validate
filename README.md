[![Snapshot Artifacts][Badge-SonatypeSnapshots]][Link-SonatypeSnapshots]

## Running the test harness

PGV repo includes a comprehensive [test suite](https://github.com/envoyproxy/protoc-gen-validate/blob/master/tests/harness/executor/cases.go) defined over these [proto files](https://github.com/envoyproxy/protoc-gen-validate/tree/master/tests/harness/cases).

To run this test suite against scalapb-validate:

1. Run `./make_harness.sh`. This will download [bazelisk](https://github.com/bazelbuild/bazelisk), clone PGV, and compile its test executor. It is only needed to be done once, and can take a few minutes to compile.

2. In SBT, run `e2e/test:runMain scalapb.validate.ScalaHarness`.

## Adding the latest snapshot release to your project

1. Note the latest snapshot version on [Sonatype Snapshots](https://oss.sonatype.org/content/repositories/snapshots/com/thesamet/scalapb/scalapb-validate-core_2.13/)

   [![Snapshot Artifacts][Badge-SonatypeSnapshots]][Link-SonatypeSnapshots]

1. Add the following to your `project/plugins.sbt`, replace the
   validateVersion value with the snapshot version you have found in the
   previous step:
   ```
   ThisBuild / resolvers += Resolver.sonatypeRepo("snapshots")

   addSbtPlugin("com.thesamet" % "sbt-protoc" % "0.99.34")

   libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.11.0-M2"

   val validateVersion = "0.1.1"

   libraryDependencies += "com.thesamet.scalapb" %% "scalapb-validate-codegen" % validateVersion
   ```

1. Add the following to your `build.sbt`:
   ```
   ThisBuild / resolvers += Resolver.sonatypeRepo("snapshots")

   PB.targets in Compile := Seq(
     scalapb.gen() -> (sourceManaged in Compile).value / "scalapb",
     scalapb.validate.gen() -> (sourceManaged in Compile).value / "scalapb"
   )

   libraryDependencies ++= Seq(
     "com.thesamet.scalapb" %% "scalapb-validate-core" % scalapb.validate.compiler.BuildInfo.version % "protobuf"
   )
   ```

1. import `validate/validate.proto` in your protobufs and set up validators as described in [protoc-gen-validate documentation](https://github.com/envoyproxy/protoc-gen-validate).

1. The generated code will generate a Validator object for each message class. For example, if you have a `Person` message, it will generate a `PersonValidator` object that has a `validate(instance: Person)` method that returns a validation [`Result`](https://github.com/scalapb/scalapb-validate/blob/master/core/src/main/scala/scalapb/validate/Validator.scala).

## Examples
See a full example at the [examples directory](https://github.com/scalapb/scalapb-validate/tree/master/example).

[Link-SonatypeSnapshots]: https://oss.sonatype.org/content/repositories/snapshots/com/thesamet/scalapb/scalapb-validate-core_2.13/ "Sonatype Snapshots"
[Badge-SonatypeSnapshots]: https://img.shields.io/nexus/s/https/oss.sonatype.org/com.thesamet.scalapb/scalapb-validate-core_2.13.svg "Sonatype Snapshots"
