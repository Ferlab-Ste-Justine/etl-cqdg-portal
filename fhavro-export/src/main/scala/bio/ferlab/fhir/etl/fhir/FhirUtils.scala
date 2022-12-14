package bio.ferlab.fhir.etl.fhir

import bio.ferlab.fhir.etl.config.Config
import bio.ferlab.fhir.etl.auth.{AuthTokenInterceptor, CookieInterceptor}
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.client.impl.GenericClient
import org.apache.http.HttpHost
import org.apache.http.client.utils.URIUtils

import java.net.URI

object FhirUtils {

  implicit val fhirContext: FhirContext = FhirContext.forR4()

  def buildFhirClient(config: Config): GenericClient = {
    val fhirClient: GenericClient = fhirContext.getRestfulClientFactory.newGenericClient(s"${config.fhirConfig.baseUrl}").asInstanceOf[GenericClient]
    fhirClient.registerInterceptor(new AuthTokenInterceptor(config.keycloakConfig))
    fhirClient
  }

  def replaceBaseUrl(url: String, replaceHost: String) = {
    val pattern = """(^http[s]?:\/\/.[^\/]+)\/?""".r
    val cleanUrl = pattern.findAllIn(replaceHost).group(1)
    URIUtils.rewriteURI(URI.create(url), HttpHost.create(cleanUrl)).toString
  }
}
