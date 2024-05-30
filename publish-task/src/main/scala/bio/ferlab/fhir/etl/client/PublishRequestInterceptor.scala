package bio.ferlab.fhir.etl

import org.apache.http.protocol.HttpContext
import org.apache.http.{HttpRequest, HttpRequestInterceptor}
import org.slf4j.{Logger, LoggerFactory}

import java.util.Base64

class PublishRequestInterceptor(esConfig: Map[String, String]) extends HttpRequestInterceptor {
  private def getBasicAuthenticationHeader(username: String, password: String): String = {
    val valueToEncode = username + ":" + password
    "Basic " + Base64.getEncoder.encodeToString(valueToEncode.getBytes)
  }

  val LOGGER: Logger = LoggerFactory.getLogger(getClass)

  override def process(request: HttpRequest, context: HttpContext): Unit = {
    (esConfig.get("es.net.http.auth.user"), esConfig.get("es.net.http.auth.pass")) match {
      case (Some(user), Some(password)) =>
        LOGGER.debug("HTTP request intercepted.  Adding Authorization header.")
        request.addHeader("Authorization", getBasicAuthenticationHeader(user, password))
      case _ =>
    }
  }
}
