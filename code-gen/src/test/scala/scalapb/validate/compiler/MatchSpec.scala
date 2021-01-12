package scalapb.validate.compiler

import io.envoyproxy.pgv.validate.Validate.FieldRules
import com.google.protobuf.TextFormat
import scalapb.compiler.GeneratorException
import scalapb.options.Scalapb.FieldOptions
import scalapb.options.Scalapb.ScalaPbOptions

class OptionsSpec extends munit.FunSuite {

  def fieldRules(s: String) = TextFormat.parse(s, classOf[FieldRules])

  def fieldOptions(s: String) = TextFormat.parse(s, classOf[FieldOptions])

  def scalapbOptions(s: String) = TextFormat.parse(s, classOf[ScalaPbOptions])

  test("presence match") {
    assertEquals(
      ProcessRequest.matchPresence(
        msg = fieldRules(""),
        pattern = fieldRules("")
      ),
      true
    )

    assertEquals(
      ProcessRequest.matchPresence(
        msg = fieldRules(""),
        pattern = fieldRules("int32: {gt: 1}")
      ),
      false
    )
    assertEquals(
      ProcessRequest.matchPresence(
        msg = fieldRules("int32: {gt: 2}"),
        pattern = fieldRules("int32: {gt: 1}")
      ),
      true
    )
    assertEquals(
      ProcessRequest.matchPresence(
        msg = fieldRules("int32: {gt: 0}"),
        pattern = fieldRules("int32: {gt: 1}")
      ),
      true
    )
    assertEquals(
      ProcessRequest.matchPresence(
        msg = fieldRules("int32: {gt: 0}"),
        pattern = fieldRules("int32: {gt: 0}")
      ),
      true
    )
    assertEquals(
      ProcessRequest.matchPresence(
        msg = fieldRules("int32: {}"),
        pattern = fieldRules("int32: {}")
      ),
      true
    )
    assertEquals(
      ProcessRequest.matchPresence(
        msg = fieldRules("int32: {}"),
        pattern = fieldRules("int32: {gt: 0}")
      ),
      false
    )
    assertEquals(
      ProcessRequest.matchPresence(
        msg = fieldRules("int32: {gt: 0}"),
        pattern = fieldRules("int32: {}")
      ),
      true
    )
    assertEquals(
      ProcessRequest.matchPresence(
        msg = fieldRules("int32: {gt: 0, lt: 0}"),
        pattern = fieldRules("int32: {gt: 0}")
      ),
      true
    )
  }

  test("fieldByPath") {
    assertEquals(
      ProcessRequest.fieldByPath(
        fieldRules("int32: {gt: 1, lt: 2}"),
        "int32.gt"
      ),
      "1"
    )
    assertEquals(
      ProcessRequest.fieldByPath(
        fieldRules("int32: {gt: 1, lt: 2}"),
        "int32.lt"
      ),
      "2"
    )
    assertEquals(
      ProcessRequest.fieldByPath(
        fieldRules("int32: {gt: 1, lt: 2}"),
        "int32.gte"
      ),
      "0"
    )
    interceptMessage[GeneratorException](
      "Could not find field named foo when looking for int32.foo"
    ) {
      ProcessRequest.fieldByPath(
        fieldRules("int32: {gt: 1, lt: 2}"),
        "int32.foo"
      )
    }
    interceptMessage[GeneratorException](
      "Type INT32 does not have a field lt in int32.gt.lt"
    ) {
      ProcessRequest.fieldByPath(
        fieldRules("int32: {gt: 1, lt: 2}"),
        "int32.gt.lt"
      )
    }
    interceptMessage[GeneratorException]("Got an empty path") {
      ProcessRequest.fieldByPath(
        fieldRules("int32: {gt: 1, lt: 2}"),
        ""
      )
    }
  }

  test("interpolateStrings") {
    assertEquals(
      ProcessRequest.interpolateStrings(
        fieldOptions("type: \"Thingie($(int32.gt))\""),
        fieldRules("int32: {gt: 1, lt: 2}")
      ),
      fieldOptions("type: \"Thingie(1)\"")
    )

    assertEquals(
      ProcessRequest.interpolateStrings(
        fieldOptions("type: \"Thingie($(int32.gt), $(int32.lt))\""),
        fieldRules("int32: {gt: 1, lt: 2}")
      ),
      fieldOptions("type: \"Thingie(1, 2)\"")
    )

    assertEquals(
      ProcessRequest.interpolateStrings(
        fieldOptions("type: \"Thingie($(int32.gte))\""),
        fieldRules("int32: {gt: 1, lt: 2}")
      ),
      fieldOptions("type: \"Thingie(0)\"")
    )

    // To test that it looks into nested fields:
    assertEquals(
      ProcessRequest.interpolateStrings(
        scalapbOptions(
          "aux_field_options { options: { type: \"Thingie($(int32.gt))\" } }"
        ),
        fieldRules("int32: {gt: 1, lt: 2}")
      ),
      scalapbOptions("aux_field_options: { options: {type: \"Thingie(1)\"} }")
    )

    interceptMessage[GeneratorException](
      "Could not find field named gtx when looking for int32.gtx"
    ) {
      ProcessRequest.interpolateStrings(
        fieldOptions("type: \"Thingie($(int32.gtx))\""),
        fieldRules("int32: {gt: 1, lt: 2}")
      )
    }

  }
}
