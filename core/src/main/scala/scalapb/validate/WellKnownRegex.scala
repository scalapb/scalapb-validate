package scalapb.validate

import com.google.re2j.Pattern

object WellKnownRegex {
  val HTTP_HEADER_NAME = Pattern.compile("^:?[0-9a-zA-Z!#$%&'*+-.^_|~`]+$")
  val HTTP_HEADER_VALUE =
    Pattern.compile("^[^\u0000-\u0008\u000A-\u001F\u007F]*$")
  val HEADER_STRING =
    Pattern.compile("^[^\u0000\u000A\u000D]*$") // For non-strict validation.
}
