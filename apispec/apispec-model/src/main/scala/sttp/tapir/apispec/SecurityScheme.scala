package sttp.tapir.apispec

import scala.collection.immutable.ListMap

case class SecurityScheme(
    `type`: String,
    description: Option[String],
    name: Option[String],
    in: Option[String],
    scheme: Option[String],
    bearerFormat: Option[String],
    flows: Option[OAuthFlows],
    openIdConnectUrl: Option[String],
    // TODO extensions
)

case class OAuthFlows(
    `implicit`: Option[OAuthFlow] = None,
    password: Option[OAuthFlow] = None,
    clientCredentials: Option[OAuthFlow] = None,
    authorizationCode: Option[OAuthFlow] = None,
    // TODO extensions
)

case class OAuthFlow(
  authorizationUrl: String,
  tokenUrl: Option[String],
  refreshUrl: Option[String],
  scopes: ListMap[String, String],
  // TODO extensions
)
