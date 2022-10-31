package bio.ferlab.fhir.etl.auth

import ca.uhn.fhir.rest.client.api.{IClientInterceptor, IHttpRequest, IHttpResponse}
import org.slf4j.{Logger, LoggerFactory}

class AuthTokenInterceptor(token: String) extends IClientInterceptor {

  val LOGGER: Logger = LoggerFactory.getLogger(getClass)

  override def interceptRequest(theRequest: IHttpRequest): Unit = {
    LOGGER.debug("HTTP request intercepted.  Adding Authorization header.")
    theRequest.addHeader("Authorization", s"Bearer $token")
  }

  override def interceptResponse(theResponse: IHttpResponse): Unit = {
    // Nothing to do here for now
  }
}
