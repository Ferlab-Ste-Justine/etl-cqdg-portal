package bio.ferlab.fhir.etl.auth

import bio.ferlab.fhir.etl.config.KeycloakConf
import bio.ferlab.fhir.etl.keycloack.Auth
import ca.uhn.fhir.rest.client.api.{IClientInterceptor, IHttpRequest, IHttpResponse}
import org.slf4j.{Logger, LoggerFactory}

class AuthTokenInterceptor(conf: KeycloakConf) extends IClientInterceptor {

  val LOGGER: Logger = LoggerFactory.getLogger(getClass)
  val auth = new Auth(conf)

  override def interceptRequest(theRequest: IHttpRequest): Unit = auth.withToken { (_, rpt) =>
    LOGGER.debug("HTTP request intercepted.  Adding Authorization header.")
    theRequest.addHeader("Authorization", s"Bearer $rpt")
  }

  override def interceptResponse(theResponse: IHttpResponse): Unit = {
    // Nothing to do here for now
  }
}
