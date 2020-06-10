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
import java.nio.file.Files
import java.nio.file.Path
import scala.sys.process.Process
import scala.jdk.CollectionConverters._
import java.nio.file.attribute.PosixFilePermission
import io.undertow.Undertow

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
  * test cases provided by PGV. To get around it, we start here an HTTP server that implements
  * the same protocol over HTTP. The executor is provided with a shell script that calls `curl`
  * to connect to the server.
  */
object ScalaHarness extends cask.MainRoutes {
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

  @cask.post("/")
  def processRequest(req: cask.Request) = {
    val testCase = TestCase.parseFrom(req.readAllBytes())
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
    result.toByteArray
  }

  initialize()

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
             |curl --url http://localhost:$port/ -s --data-binary @-
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

  override val port = {
    val ss = new ServerSocket(0)
    try ss.getLocalPort()
    finally ss.close()
  }

  override def main(args: Array[String]): Unit = {
    val server = Undertow.builder
      .addHttpListener(port, host)
      .setHandler(defaultHandler)
      .build
    Future(server.start())
    val script = createScript(port)
    val status =
      try Process(
        ".pgv/executor.exe",
        Seq(
          "-external_harness",
          script.toString()
        )
      ).!
      finally {
        Files.delete(script)
        server.stop()
      }

    sys.exit(status)
  }
}
