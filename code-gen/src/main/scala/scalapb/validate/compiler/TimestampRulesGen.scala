package scalapb.validate.compiler

import io.envoyproxy.pgv.validate.validate.TimestampRules
import scalapb.compiler.FunctionApplication

object TimestampRulesGen {
  val TV = "scalapb.validate.TimestampValidation"
  def timestampRules(rules: TimestampRules) =
    Seq(
      rules.within.map { d =>
        Rule.java(
          "io.envoyproxy.pgv.TimestampValidation.within",
          FunctionApplication(
            "com.google.protobuf.timestamp.Timestamp.toJavaProto"
          ),
          s"io.envoyproxy.pgv.TimestampValidation.toDuration(${d.seconds}, ${d.nanos})",
          "io.envoyproxy.pgv.TimestampValidation.currentTimestamp()"
        )
      },
      Rule.ifSet(rules.getLtNow)(
        Rule
          .basic(
            ComparativeRulesGen.CV + ".lessThan",
            s"$TV.currentTimestamp()"
          )
          .withImport(ComparativeRulesGen.TimestampOrdering)
      ),
      Rule.ifSet(rules.getGtNow)(
        Rule
          .basic(
            ComparativeRulesGen.CV + ".greaterThan",
            s"$TV.currentTimestamp()"
          )
          .withImport(ComparativeRulesGen.TimestampOrdering)
      ),
      rules.within.map { d =>
        Rule.java(
          "io.envoyproxy.pgv.TimestampValidation.within",
          FunctionApplication(
            "com.google.protobuf.timestamp.Timestamp.toJavaProto"
          ),
          s"io.envoyproxy.pgv.TimestampValidation.toDuration(${d.seconds}, ${d.nanos})",
          "io.envoyproxy.pgv.TimestampValidation.currentTimestamp()"
        )
      }
    ).flatten

}
