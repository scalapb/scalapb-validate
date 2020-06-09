[![Snapshot Artifacts][Badge-SonatypeSnapshots]][Link-SonatypeSnapshots]

## Running the test harness

PGV repo includes a comprehensive [test suite](https://github.com/envoyproxy/protoc-gen-validate/blob/master/tests/harness/executor/cases.go) defined over these [proto files](https://github.com/envoyproxy/protoc-gen-validate/tree/master/tests/harness/cases).

To run this test suite against scalapb-validate:

1. Run `./make_harness.sh`. This will download [bazelisk](https://github.com/bazelbuild/bazelisk), clone PGV, and compile its test executor. It is only needed to be done once, and can take a few minutes to compile.

2. In SBT, run `e2e/test:runMain scalapb.validate.ScalaHarness`.

## Examples
See a full example at the [examples directory](https://github.com/scalapb/scalapb-validate/tree/master/examples).

[Link-SonatypeSnapshots]: https://oss.sonatype.org/content/repositories/snapshots/com/thesamet/scalapb/scalapb-validate-core_2.13/ "Sonatype Snapshots"
[Badge-SonatypeSnapshots]: https://img.shields.io/nexus/s/https/oss.sonatype.org/com.thesamet.scalapb/scalapb-validate-core_2.13.svg "Sonatype Snapshots"
