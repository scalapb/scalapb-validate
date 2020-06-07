package scalapb.validate

import com.google.protobuf.Descriptors.FieldDescriptor
import io.envoyproxy.pgv.validate.Validate.StringRules

/**
  * StringRulesGenerator helps generate the validation code for protocol buffer string typed field
  *
  */
object StringRulesPrinter {
  private val STRING_VALIDATION: String = "io.envoyproxy.pgv.StringValidation"

  private def printLengthRules(
      fieldDescriptor: FieldDescriptor,
      inputExpr: String,
      rules: StringRules
  ) =
    if (rules.hasLen)
      Some(
        s"""$STRING_VALIDATION.length("${fieldDescriptor.getName}", $inputExpr, ${rules.getLen}")"""
      )
    else None

  private def printLenBytesRules(
      fieldDescriptor: FieldDescriptor,
      inputExpr: String,
      rules: StringRules
  ) =
    if (rules.hasLenBytes)
      Some(
        s"""$STRING_VALIDATION.lengthBytes("${fieldDescriptor.getName}", $inputExpr, ${rules.getLenBytes}")"""
      )
    else None

  private def printEmailRules(
      fieldDescriptor: FieldDescriptor,
      inputExpr: String,
      rules: StringRules
  ) =
    if (rules.getEmail)
      Some(
        s"""$STRING_VALIDATION.email("${fieldDescriptor.getName}", $inputExpr)"""
      )
    else None

  private def printPrefixRules(
      fieldDescriptor: FieldDescriptor,
      inputExpr: String,
      rules: StringRules
  ) =
    if (rules.hasPrefix)
      Some(
        s"""$STRING_VALIDATION.prefix("${fieldDescriptor.getName}", $inputExpr, ${rules.getPrefix})"""
      )
    else None

  private def printSuffixRules(
      fieldDescriptor: FieldDescriptor,
      inputExpr: String,
      rules: StringRules
  ) =
    if (rules.hasSuffix)
      Some(
        s"""$STRING_VALIDATION.suffix("${fieldDescriptor.getName}", $inputExpr, ${rules.getSuffix})"""
      )
    else None

  private def printContainsRules(
      fieldDescriptor: FieldDescriptor,
      inputExpr: String,
      rules: StringRules
  ) =
    if (rules.hasContains)
      Some(
        s"""$STRING_VALIDATION.contains("${fieldDescriptor.getName}", $inputExpr, ${rules.getContains})"""
      )
    else None

  private def printNotContainsRules(
      fieldDescriptor: FieldDescriptor,
      inputExpr: String,
      rules: StringRules
  ) =
    if (rules.hasNotContains)
      Some(
        s"""$STRING_VALIDATION.notContains("${fieldDescriptor.getName}", $inputExpr, ${rules.getNotContains})"""
      )
    else None

  private def printHostnameRules(
      fieldDescriptor: FieldDescriptor,
      inputExpr: String,
      rules: StringRules
  ) =
    if (rules.getHostname)
      Some(
        s"""$STRING_VALIDATION.hostName("${fieldDescriptor.getName}", $inputExpr)"""
      )
    else None

  private def printIpRules(
      fieldDescriptor: FieldDescriptor,
      inputExpr: String,
      rules: StringRules
  ) =
    if (rules.getIp)
      Some(
        s"""$STRING_VALIDATION.ip("${fieldDescriptor.getName}", $inputExpr)"""
      )
    else None

  private def printIpv4Rules(
      fieldDescriptor: FieldDescriptor,
      inputExpr: String,
      rules: StringRules
  ) =
    if (rules.getIpv4)
      Some(
        s"""$STRING_VALIDATION.ipv4("${fieldDescriptor.getName}", $inputExpr)"""
      )
    else None

  private def printIpv6Rules(
      fieldDescriptor: FieldDescriptor,
      inputExpr: String,
      rules: StringRules
  ) =
    if (rules.getIpv6)
      Some(
        s"""$STRING_VALIDATION.ipv6("${fieldDescriptor.getName}", $inputExpr)"""
      )
    else None

  private def printUriRules(
      fieldDescriptor: FieldDescriptor,
      inputExpr: String,
      rules: StringRules
  ) =
    if (rules.getUri)
      Some(
        s"""$STRING_VALIDATION.uri("${fieldDescriptor.getName}", $inputExpr)"""
      )
    else None

  private def printUriRefRules(
      fieldDescriptor: FieldDescriptor,
      inputExpr: String,
      rules: StringRules
  ) =
    if (rules.getUriRef)
      Some(
        s"""$STRING_VALIDATION.uriRef("${fieldDescriptor.getName}", $inputExpr)"""
      )
    else None

  private def printUuidRules(
      fieldDescriptor: FieldDescriptor,
      inputExpr: String,
      rules: StringRules
  ) =
    if (rules.getUuid)
      Some(
        s"""$STRING_VALIDATION.uuid("${fieldDescriptor.getName}", $inputExpr)"""
      )
    else None

  private def printAddressRules(
      fieldDescriptor: FieldDescriptor,
      inputExpr: String,
      rules: StringRules
  ) =
    if (rules.getAddress)
      Some(
        s"""$STRING_VALIDATION.address("${fieldDescriptor.getName}", $inputExpr)"""
      )
    else None

  private def printMaxBytesRules(
      fieldDescriptor: FieldDescriptor,
      inputExpr: String,
      rules: StringRules
  ) =
    if (rules.hasMaxBytes)
      Option(
        s"""$STRING_VALIDATION.maxBytes("${fieldDescriptor.getName}", $inputExpr, ${rules.getMaxBytes})"""
      )
    else None

  private def printMiniBytesRules(
      fieldDescriptor: FieldDescriptor,
      inputExpr: String,
      rules: StringRules
  ) =
    if (rules.hasMinBytes)
      Option(
        s"""$STRING_VALIDATION.minBytes("${fieldDescriptor.getName}", $inputExpr, ${rules.getMinBytes})"""
      )
    else None

  private def printMiniLengthRules(
      fieldDescriptor: FieldDescriptor,
      inputExpr: String,
      rules: StringRules
  ) =
    if (rules.hasMinLen)
      Option(
        s"""$STRING_VALIDATION.minLength("${fieldDescriptor.getName}", $inputExpr, ${rules.getMinLen})"""
      )
    else None

  // Need to find way to compile the pattern only once
  private def printPatternRules(
      fieldDescriptor: FieldDescriptor,
      inputExpr: String,
      rules: StringRules
  ) =
    if (rules.hasPattern)
      Option(
        s"""$STRING_VALIDATION.pattern("${fieldDescriptor.getName}", $inputExpr, com.google.re2j.Pattern.compile("${rules.getPattern}"))"""
      )
    else None

  private def printMaxLengthRules(
      fieldDescriptor: FieldDescriptor,
      inputExpr: String,
      rules: StringRules
  ) =
    if (rules.hasMaxLen)
      Option(
        s"""$STRING_VALIDATION.maxLength("${fieldDescriptor.getName}", $inputExpr, ${rules.getMaxLen})"""
      )
    else None

  def print(
      fieldDescriptor: FieldDescriptor,
      inputExpr: String,
      rules: StringRules
  ): Iterable[String] =
    Seq(
      printAddressRules(fieldDescriptor, inputExpr, rules),
      printContainsRules(fieldDescriptor, inputExpr, rules),
      printEmailRules(fieldDescriptor, inputExpr, rules),
      printHostnameRules(fieldDescriptor, inputExpr, rules),
      printIpRules(fieldDescriptor, inputExpr, rules),
      printIpv4Rules(fieldDescriptor, inputExpr, rules),
      printIpv6Rules(fieldDescriptor, inputExpr, rules),
      printLenBytesRules(fieldDescriptor, inputExpr, rules),
      printLengthRules(fieldDescriptor, inputExpr, rules),
      printMaxBytesRules(fieldDescriptor, inputExpr, rules),
      printMaxLengthRules(fieldDescriptor, inputExpr, rules),
      printMiniBytesRules(fieldDescriptor, inputExpr, rules),
      printMiniLengthRules(fieldDescriptor, inputExpr, rules),
      printNotContainsRules(fieldDescriptor, inputExpr, rules),
      printPatternRules(fieldDescriptor, inputExpr, rules),
      printPrefixRules(fieldDescriptor, inputExpr, rules),
      printSuffixRules(fieldDescriptor, inputExpr, rules),
      printUriRefRules(fieldDescriptor, inputExpr, rules),
      printUriRules(fieldDescriptor, inputExpr, rules),
      printUuidRules(fieldDescriptor, inputExpr, rules)
    ).flatten
}
