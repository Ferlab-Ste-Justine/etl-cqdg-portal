package bio.ferlab.fhir.etl

import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpRequestBase
import org.apache.http.impl.client.{CloseableHttpClient, HttpClientBuilder}
import org.apache.http.util.EntityUtils

class EsHttpClient(esConfig: Map[String, String]) {

  val httpBuilder: HttpClientBuilder =
    HttpClientBuilder.create()
    .addInterceptorFirst(new PublishRequestInterceptor(esConfig))

  val http: CloseableHttpClient = httpBuilder.build()
  val charsetUTF8 = "UTF-8"
  sys.addShutdownHook(http.close())

  def executeHttpRequest(request: HttpRequestBase): (Option[String], Int) = {
    val response: HttpResponse = http.execute(request)
    val body = Option(response.getEntity).map(e => EntityUtils.toString(e, charsetUTF8))
    // always properly close
    EntityUtils.consumeQuietly(response.getEntity)
    (body, response.getStatusLine.getStatusCode)
  }

}
