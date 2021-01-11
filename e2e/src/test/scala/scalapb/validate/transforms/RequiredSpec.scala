package scalapb.validate.transforms

import com.google.protobuf.InvalidProtocolBufferException
import scalapb.transforms.required.{Container, RequiredMsg}
import scalapb.json4s.JsonFormat

class RequiredSpec extends munit.FunSuite {

  test("Container should have unboxed requiredMsg") {
    val c = Container(requiredMsg = RequiredMsg()) // <-- not in an option
    assertEquals(Container.parseFrom(c.toByteArray), c)
  }

  test("Container should fail parsing empty message") {
    interceptMessage[InvalidProtocolBufferException](
      "Message missing required fields."
    ) {
      Container.parseFrom(Array.empty[Byte])
    }
  }

  test("Container should serialize from/to json") {
    val c = Container(requiredMsg = RequiredMsg()) // <-- not in an option
    assertEquals(
      JsonFormat.fromJsonString[Container](
        JsonFormat.toJsonString(c)
      ),
      c
    )
  }
}
