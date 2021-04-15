package sttp.tapir.apispec

case class Reference($ref: String)

object Reference {
  def to(prefix: String, $ref: String): Reference = new Reference(s"$prefix${$ref}")
}

sealed trait ExampleValue

case class ExampleSingleValue(value: String) extends ExampleValue
case class ExampleMultipleValue(values: List[String]) extends ExampleValue

case class Tag(
  name: String,
  description: Option[String] = None,
  externalDocs: Option[ExternalDocumentation] = None,
  // TODO extensions
)

case class ExternalDocumentation(url: String, description: Option[String] = None)
