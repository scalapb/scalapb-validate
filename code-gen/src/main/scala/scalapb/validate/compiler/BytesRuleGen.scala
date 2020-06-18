package scalapb.validate.compiler

import com.google.protobuf.Descriptors.FieldDescriptor
import io.envoyproxy.pgv.validate.validate.BytesRules
import scalapb.validate.compiler.Rule.ifSet
import scalapb.validate.compiler.StringRulesGen.quoted

object BytesRuleGen {
  private val BV: String = "io.envoyproxy.pgv.BytesValidation"

  def bytesRules(
      fd: FieldDescriptor,
      rules: BytesRules
  ): Seq[Rule] =
    Seq(
      rules.len.map(value => Rule.java(s"$BV.length", value.toString)),
      rules.minLen.map(value => Rule.java(s"$BV.minLength", value.toString)),
      rules.maxLen.map(value => Rule.java(s"$BV.maxLength", value.toString)),
      /*      rules.prefix.map((v: ByteString) =>
        Rule.java(
          s"$BV.prefix",
          s" java.util.Arrays.toString(${v.toByteArray})"
        )
      ),
      rules.suffix.map(v =>
        Rule.java(
          s"$BV.suffix",
          s"${v.toByteArray.clone()}"
        )
      ),*/
      rules.pattern.map { v =>
        val pname = s"Pattern_${fd.getName}"
        Rule
          .java(
            s"$BV.pattern",
            pname
          )
          .withPreamble(
            s"private val $pname = com.google.re2j.Pattern.compile(${quoted(v)})"
          )
      },
      ifSet(rules.getIp)(Rule.java(s"$BV.ip")),
      ifSet(rules.getIpv4)(Rule.java(s"$BV.ipv4")),
      ifSet(rules.getIpv6)(Rule.java(s"$BV.ipv6"))
      /*      rules.contains.map(v =>
        Rule
          .java(
            s"$BV.contains",
            s"${v.toByteArray}.getBytes(StandardCharsets.UTF_8)"
          )
      )*/
    ).flatten ++ MembershipRulesGen.membershipRules(rules)
}
