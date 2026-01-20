package bio.ferlab.fhir.etl.s3

import bio.ferlab.fhir.etl.config.{Config, FhirRequest}
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.http.apache.ApacheHttpClient
import software.amazon.awssdk.services.s3.model.{HeadObjectRequest, NoSuchKeyException, PutObjectRequest}
import software.amazon.awssdk.services.s3.{S3Client, S3Configuration}

import java.io.File
import java.net.URI

object S3Utils {

  def buildS3Client(config: Config): S3Client = {

    val confBuilder: S3Configuration = S3Configuration
      .builder()
      .pathStyleAccessEnabled(config.awsConfig.pathStyleAccess)
      .build()

    val s3Builder = S3Client
      .builder()
      .credentialsProvider(DefaultCredentialsProvider.create())
      .httpClient(ApacheHttpClient.create())
      .serviceConfiguration(confBuilder)

    if (config.awsConfig.endpoint.isDefined) {
      val endpointUri = new URI(config.awsConfig.endpoint.get)
      s3Builder.endpointOverride(endpointUri)
    }

    s3Builder.build()

  }

  def writeFile(bucket: String, key: String, file: File)(implicit s3Client: S3Client): Unit = {
    val objectRequest = PutObjectRequest
      .builder()
      .bucket(bucket)
      .key(key)
      .build()

    s3Client.putObject(objectRequest, RequestBody.fromFile(file))
  }

  def exists(bucket: String, key: String)(implicit s3Client: S3Client): Boolean = {
    try {
      s3Client.headObject(
        HeadObjectRequest.builder
          .bucket(bucket)
          .key(key)
          .build
      )
      true
    } catch {
      case _: NoSuchKeyException =>
        false
    }
  }

  def buildKey(fhirRequest: FhirRequest, studyId: String): String = {
    val profilePath = fhirRequest.entityType.getOrElse(fhirRequest.`type`.toLowerCase())

    s"fhir/$profilePath/study_id=$studyId/${fhirRequest.schema}.avro"
  }
}
