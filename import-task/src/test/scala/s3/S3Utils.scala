package s3

import conf.AWSConf
import software.amazon.awssdk.auth.credentials.{AwsBasicCredentials, StaticCredentialsProvider}
import software.amazon.awssdk.http.apache.ApacheHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.{S3Client, S3Configuration}

import java.net.URI

object S3Utils {


  def buildS3Client(conf: AWSConf): S3Client = {
    val confBuilder: S3Configuration = software.amazon.awssdk.services.s3.S3Configuration.builder()
      .pathStyleAccessEnabled(conf.pathStyleAccess)
      .build()
    val staticCredentialsProvider: StaticCredentialsProvider = StaticCredentialsProvider.create(
      AwsBasicCredentials.create(conf.accessKey, conf.secretKey)
    )
    val endpoint = URI.create(conf.endpoint)
    val s3: S3Client = S3Client.builder()
      .credentialsProvider(staticCredentialsProvider)
      .endpointOverride(endpoint)
      .region(Region.US_EAST_1)
      .serviceConfiguration(confBuilder)
      .httpClient(ApacheHttpClient.create())
      .build()
    s3
  }

  def getContent(bucket: String, key: String)(implicit s3Client: S3Client): String = {
    val objectRequest = GetObjectRequest
      .builder()
      .key(key)
      .bucket(bucket)
      .build()

    new String(s3Client.getObject(objectRequest).readAllBytes())
  }



}
