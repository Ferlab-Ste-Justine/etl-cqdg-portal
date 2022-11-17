package bio.ferlab.fhir.etl.keycloack

import bio.ferlab.fhir.etl.config.KeycloakConf
import org.keycloak.authorization.client.{AuthzClient, Configuration}
import org.keycloak.common.util.Time
import org.keycloak.representations.idm.authorization.AuthorizationRequest

import scala.jdk.CollectionConverters._

class Auth(conf: KeycloakConf) {

  private val config = new Configuration()
  config.setRealm(conf.realm)
  config.setAuthServerUrl(conf.url)
  config.setResource(conf.clientKey)
  config.setCredentials(Map("secret" -> conf.clientSecret).toMap[String, Object].asJava)
  private val authzClient = AuthzClient.create(config)

  private val req = new AuthorizationRequest()
  req.setAudience(conf.audience)
  private var expiresAt = 0L
  private var rpt = ""
  private var accessToken = ""

  def withToken[T](f: (String, String) => T): T = {

    if (expiresAt == 0 || expiresAt < Time.currentTime()) {
      accessToken = authzClient.obtainAccessToken().getToken
      val resp = authzClient.authorization().authorize(req)
      val expiresIn = resp.getExpiresIn
      expiresAt = Time.currentTime() + expiresIn - 5
      rpt = resp.getToken
    }
    f(accessToken, rpt)
  }


}
