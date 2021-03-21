package scalapb.validate.compiler

import io.envoyproxy.pgv.validate.validate.StringRules
import Rule._
import com.google.protobuf.Descriptors.FieldDescriptor

/** StringRulesGenerator helps generate the validation code for protocol buffer string typed field
  */
object StringRulesGen {
  private val SV: String = "io.envoyproxy.pgv.StringValidation"
  private val CV: String = "io.envoyproxy.pgv.ConstantValidation"
  private val WRX: String = "scalapb.validate.WellKnownRegex"

  // Copied from ScalaPB's ProtobufGenerator
  // TODO: move to common place
  private[validate] def quoted(raw: String): String =
    raw
      .map {
        case '\b'                      => "\\b"
        case '\f'                      => "\\f"
        case '\n'                      => "\\n"
        case '\r'                      => "\\r"
        case '\t'                      => "\\t"
        case '\\'                      => "\\\\"
        case '\"'                      => "\\\""
        case '\''                      => "\\\'"
        case u if u >= ' ' && u <= '~' => u.toString
        case c: Char                   => "\\u%4s".format(c.toInt.toHexString).replace(' ', '0')
      }
      .mkString("\"", "", "\"")

  def stringRules(
      fd: FieldDescriptor,
      rules: StringRules
  ): Seq[Rule] =
    Seq(
      rules.len.map(value => Rule.java(s"$SV.length", value.toString)),
      rules.lenBytes.map(value => Rule.java(s"$SV.lenBytes", value.toString)),
      rules.minLen.map(value => Rule.java(s"$SV.minLength", value.toString)),
      rules.maxLen.map(value => Rule.java(s"$SV.maxLength", value.toString)),
      rules.minBytes.map(value => Rule.java(s"$SV.minBytes", value.toString)),
      rules.maxBytes.map(value => Rule.java(s"$SV.maxBytes", value.toString)),
      rules.contains.map(v => Rule.java(s"$SV.contains", quoted(v))),
      rules.notContains.map(v => Rule.java(s"$SV.notContains", quoted(v))),
      rules.const.map(v => Rule.java(s"$CV.constant", quoted(v))),
      rules.prefix.map(v => Rule.java(s"$SV.prefix", quoted(v))),
      rules.suffix.map(v => Rule.java(s"$SV.suffix", quoted(v))),
      rules.pattern.map { v =>
        val pname = s"Pattern_${fd.getName()}"
        Rule
          .java(
            s"$SV.pattern",
            pname
          )
          .withPreamble(
            s"private val $pname = com.google.re2j.Pattern.compile(${quoted(v)})"
          )
      },
      ifSet(rules.getAddress)(Rule.java(s"$SV.address")),
      ifSet(rules.getEmail)(Rule.java(s"$SV.email")),
      ifSet(rules.getHostname)(Rule.java(s"$SV.hostName")),
      ifSet(rules.getIp)(Rule.java(s"$SV.ip")),
      ifSet(rules.getIpv4)(Rule.java(s"$SV.ipv4")),
      ifSet(rules.getIpv6)(Rule.java(s"$SV.ipv6")),
      ifSet(rules.getUri)(Rule.java(s"$SV.uri")),
      ifSet(rules.getUriRef)(Rule.java(s"$SV.uriRef")),
      ifSet(rules.getUuid)(Rule.java(s"$SV.uuid")),
      ifSet(rules.getWellKnownRegex.isHttpHeaderName && rules.getStrict)(
        Rule.java(s"$SV.pattern", s"$WRX.HTTP_HEADER_NAME")
      ),
      ifSet(rules.getWellKnownRegex.isHttpHeaderValue && rules.getStrict)(
        Rule.java(s"$SV.pattern", s"$WRX.HTTP_HEADER_VALUE")
      ),
      ifSet(
        (rules.getWellKnownRegex.isHttpHeaderValue || rules.getWellKnownRegex.isHttpHeaderName) && !rules.getStrict
      )(
        Rule.java(s"$SV.pattern", s"$WRX.HEADER_STRING")
      )
    ).flatten ++ MembershipRulesGen.membershipRules(rules)
}
