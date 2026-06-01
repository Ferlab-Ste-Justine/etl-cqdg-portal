package bio.ferlab.fhir.etl

import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpRequestBase
import org.apache.http.impl.client.{CloseableHttpClient, HttpClientBuilder}
import org.apache.http.ssl.SSLContexts
import org.apache.http.util.EntityUtils

import java.nio.file.{Files, Path, Paths}
import java.security.KeyStore
import java.security.cert.CertificateFactory
import javax.net.ssl.SSLContext

class EsHttpClient(esConfig: Map[String, String]) {

  // CA certificate is provided by the infrastructure (mounted from a Kubernetes
  // secret into the pod). The program only reads it from this folder; when it is
  // absent we fall back to the JVM default trust store.
  private val caCertPath: String = sys.env.getOrElse("ES_CACERT", "/opt/opensearch-ca/ca.crt")

  val httpBuilder: HttpClientBuilder =
    HttpClientBuilder
      .create()
      .addInterceptorFirst(new PublishRequestInterceptor(esConfig))

  buildSslContext(Paths.get(caCertPath)).foreach(httpBuilder.setSSLContext)

  val http: CloseableHttpClient = httpBuilder.build()
  val charsetUTF8 = "UTF-8"
  sys.addShutdownHook(http.close())

  /** Builds an SSLContext trusting the CA certificate found at `path`, or None when no usable certificate is present
    * (so the default JVM trust store is used).
    */
  private def buildSslContext(path: Path): Option[SSLContext] = {
    if (Files.isRegularFile(path) && Files.size(path) > 0) {
      println(s"Loading OpenSearch CA certificate from $path")
      val in = Files.newInputStream(path)
      val cert =
        try CertificateFactory.getInstance("X.509").generateCertificate(in)
        finally in.close()

      val trustStore = KeyStore.getInstance(KeyStore.getDefaultType)
      trustStore.load(null, null)
      trustStore.setCertificateEntry("opensearch-ca", cert)

      Some(SSLContexts.custom().loadTrustMaterial(trustStore, null).build())
    } else {
      println(s"No CA certificate found at $path, using default JVM trust store")
      None
    }
  }

  def executeHttpRequest(request: HttpRequestBase): (Option[String], Int) = {
    val response: HttpResponse = http.execute(request)
    val body = Option(response.getEntity).map(e => EntityUtils.toString(e, charsetUTF8))
    // always properly close
    EntityUtils.consumeQuietly(response.getEntity)
    (body, response.getStatusLine.getStatusCode)
  }

}
