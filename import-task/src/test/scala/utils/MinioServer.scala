package utils


import conf.AWSConf
import s3.S3Utils
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, TestSuite}
import org.slf4j.{Logger, LoggerFactory}
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.{CreateBucketRequest, DeleteObjectRequest, ListObjectsRequest, PutObjectRequest}

import java.io.File
import scala.jdk.CollectionConverters._
import scala.util.Random

trait MinioServer {
  private val (minioPort, ipAddress, _) = MinioContainer.startIfNotRunning()

  val BUCKETNAME = "cqdg-qa-app-clinical-data-service"
  val BUCKET_FHIR_IMPORT = "cqdg-ops-app-fhir-import-file-data"
  val outputBucket = "cqdg-qa-app-file-download"


  protected val minioEndpoint = s"http://localhost:$minioPort"
  protected val apiAddress: String = ipAddress
  println(minioPort)
  implicit val s3: S3Client = S3Utils.buildS3Client(AWSConf(MinioContainer.accessKey, MinioContainer.secretKey, minioEndpoint, bucketName = BUCKETNAME, pathStyleAccess = true, "", "", ""))
  val LOGGER: Logger = LoggerFactory.getLogger(getClass)

  createBuckets()

  private def createBuckets(): Unit = {
    val alreadyExistingBuckets = s3.listBuckets().buckets().asScala.collect { case b if b.name() == BUCKETNAME || b.name() == outputBucket || b.name() == BUCKET_FHIR_IMPORT => b.name() }
    val bucketsToCreate = Seq(BUCKETNAME, outputBucket, BUCKET_FHIR_IMPORT).diff(alreadyExistingBuckets)
    bucketsToCreate.foreach { b =>
      val buketRequest = CreateBucketRequest.builder().bucket(b).build()
      s3.createBucket(buketRequest)
    }
  }


  def withS3Objects[T](block: (String, String) => T): Unit = {
    val inputPrefix = s"run_${Random.nextInt(10000)}"
    LOGGER.info(s"Use input prefix $inputPrefix : $minioEndpoint/minio/$BUCKETNAME/$inputPrefix")
    val outputPrefix = s"files_${Random.nextInt(10000)}"
    LOGGER.info(s"Use output prefix $outputPrefix : : $minioEndpoint/minio/$outputBucket/$outputPrefix")
    try {
      block(inputPrefix, outputPrefix)
    } finally {
      deleteRecursively(BUCKETNAME, inputPrefix)
      deleteRecursively(outputBucket, outputPrefix)
    }
  }

  def list(bucket: String, prefix: String): Seq[String] = {
    val lsRequest = ListObjectsRequest.builder().bucket(bucket).prefix(prefix).build()
    s3.listObjects(lsRequest).contents().asScala.map(_.key()).toSeq
  }

  private def deleteRecursively(bucket: String, prefix: String): Unit = {
    val lsRequest = ListObjectsRequest.builder().bucket(bucket).prefix(prefix).build()
    s3.listObjects(lsRequest).contents().asScala.foreach { o =>
      val del = DeleteObjectRequest.builder().bucket(bucket).key(prefix).build()
      s3.deleteObject(del)
    }
  }

  def ls(file: File): List[File] = {
    file.listFiles.filter(_.isFile).toList
  }

  def transferFromResourceDirectory(prefix: String, directory: String, bucket: String = BUCKETNAME): Unit = {
    val files = ls(new File(getClass.getResource(s"/$directory").toURI))
    files.foreach { f =>
      val put = PutObjectRequest.builder().bucket(bucket).key(s"$prefix/${f.getName}").build()
      s3.putObject(put, RequestBody.fromFile(f))
    }
  }

  def transferFromResource(prefix: String, resource: String, bucket: String = BUCKETNAME): Unit = {
    val f = new File(getClass.getResource(s"/$resource").toURI)
    val put = PutObjectRequest.builder().bucket(bucket).key(s"$prefix/${f.getName}").build()
    s3.putObject(put, RequestBody.fromFile(f))
  }

  def transferFromResources(prefix: String, resource: String, bucket: String = BUCKETNAME): Unit = {
    val files = ls(new File(getClass.getResource(s"/$resource").toURI))
    files.foreach { f =>
      val put = PutObjectRequest.builder().bucket(bucket).key(s"$prefix/${f.getName}").build()
      s3.putObject(put, RequestBody.fromFile(f))
    }
  }

  def copyNFile(prefix: String, resource: String, times: Int, bucket: String = BUCKETNAME): Unit = {
    val file = new File(getClass.getResource(s"/$resource").toURI)
    1.to(times).map { i =>
      val filename = s"${file.getName}_$i"
      val put = PutObjectRequest.builder().bucket(bucket).key(s"$prefix/$filename").build()
      s3.putObject(put, RequestBody.fromFile(file))
    }
  }
}


trait MinioServerSuite extends MinioServer with TestSuite with BeforeAndAfterAll with BeforeAndAfter {

}

object StartMinioServer extends App with MinioServer {
  LOGGER.info(s"Minio is started : $minioEndpoint")
  while (true) {

  }

}
