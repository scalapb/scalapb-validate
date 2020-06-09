package scalapb.validate

import scalapb.GeneratedFileObject
import tests.harness.cases.bool.BoolProto
import tests.harness.cases.bytes.BytesProto
import tests.harness.cases.enums.EnumsProto
import tests.harness.cases.kitchen_sink.KitchenSinkProto
import tests.harness.cases.maps.MapsProto
import tests.harness.cases.messages.MessagesProto
import tests.harness.cases.numbers.NumbersProto
import tests.harness.cases.oneofs.OneofsProto
import tests.harness.cases.repeated.RepeatedProto
import tests.harness.cases.strings.StringsProto
import tests.harness.cases.wkt_any.WktAnyProto
import tests.harness.cases.wkt_duration.WktDurationProto
import tests.harness.cases.wkt_timestamp.WktTimestampProto
import tests.harness.cases.wkt_wrappers.WktWrappersProto
import tests.harness.cases.other_package.embed.EmbedProto
import scalapb.GeneratedMessageCompanion
import tests.harness.harness.TestCase
import java.net.ServerSocket
import tests.harness.harness.TestResult
import scalapb.GeneratedMessage
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import java.nio.file.Files
import java.nio.file.Path
import scala.sys.process.Process
import scala.jdk.CollectionConverters._
import java.nio.file.attribute.PosixFilePermission

/**
  * How this works?
  *
  * PGV test harness includes a go program (executor) that defines a protocol for
  * running the test cases against an arbitrary validator that can be implemented
  * in any programming language.
  *
  * For each test case, the executor launches a program (provided to it as a
  * binary), and sends an instance of a proto (wrapped in an Any) to the stdin of that
  * program. The program validates the instance and returns back a "TestResult" containing
  * the validity of the instance. The executor checks if the program was correct and prints
  * a test summary.
  *
  * Done naively, this approach would require invoking the JVM (or SBT) for each of the 900+
  * test cases provided by PGV. To get around it, we start here a TCP server that runs the
  * same protocol of TCP connections. The executor is provided with a shell script that
  * uses `netcat`.
  *
  * For simplicity, this program can only handle one connection at a time, though the executor
  * is capable of making parallel requests. However, improvements are currently unnecessary
  * as this entire suite finishes under 1s on my workstation.
  */
object ScalaHarness {
  val files = Seq(
    BoolProto,
    BytesProto,
    EnumsProto,
    KitchenSinkProto,
    MapsProto,
    MessagesProto,
    NumbersProto,
    OneofsProto,
    RepeatedProto,
    StringsProto,
    WktAnyProto,
    WktDurationProto,
    WktTimestampProto,
    WktWrappersProto,
    EmbedProto
  )

  def allMessages(
      fileObject: GeneratedFileObject
  ): Seq[GeneratedMessageCompanion[_ <: GeneratedMessage]] =
    fileObject.messagesCompanions.flatMap(m => m +: allMessages(m))

  def allMessages(
      cmp: GeneratedMessageCompanion[_]
  ): Seq[GeneratedMessageCompanion[_ <: GeneratedMessage]] =
    cmp.nestedMessagesCompanions.flatMap(m => m +: allMessages(m))

  val messages = files.flatMap(allMessages)

  val typeMap: Map[String, GeneratedMessageCompanion[_ <: GeneratedMessage]] =
    messages.map { cmp: GeneratedMessageCompanion[_ <: GeneratedMessage] =>
      (cmp.scalaDescriptor.fullName, cmp)
    }.toMap

  def serverProcess(ss: ServerSocket) =
    while (true) {
      val client = ss.accept()
      val testCase = TestCase.parseFrom(client.getInputStream())
      val message = testCase.getMessage.typeUrl.substring(20)
      val cmp = typeMap.find(_._1 == message).get._2
      val klass = Class.forName(
        cmp.defaultInstance.getClass().getCanonicalName() + "Validator$"
      )
      val vtor = klass
        .getField("MODULE$")
        .get(null)
        .asInstanceOf[Validator[GeneratedMessage]]
      val inst = testCase.getMessage.unpack(cmp)
      val result = vtor.validate(inst) match {
        case Success     => TestResult(valid = true)
        case Failure(ex) => TestResult(valid = false, reason = ex.getMessage())
      }

      val out = client.getOutputStream()
      out.write(result.toByteArray)
      client.close()
    }

  def createScript(port: Int): Path = {
    val fileName = Files.createTempFile("spv-", ".sh")
    val os = Files.newOutputStream(fileName)
    os.write(s"""#!/usr/bin/env bash
             |set -e
             |nc -N localhost $port
             """.stripMargin.getBytes("UTF-8"))
    os.close()
    Files.setPosixFilePermissions(
      fileName,
      Set(
        PosixFilePermission.OWNER_EXECUTE,
        PosixFilePermission.OWNER_READ
      ).asJava
    )
    fileName
  }

  def main(args: Array[String]): Unit = {
    val ss = new ServerSocket(0)
    val port = ss.getLocalPort()
    val script = createScript(port)
    val status =
      try {
        Future(serverProcess(ss))
        Process(
          ".pgv/executor.exe",
          Seq(
            "-external_harness",
            script.toString()
          )
        ).!
      } finally {
        ss.close()
        Files.delete(script)
      }
    sys.exit(status)
  }
}
