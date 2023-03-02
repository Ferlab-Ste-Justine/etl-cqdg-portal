package bio.ferlab.fhir.etl

import bio.ferlab.datalake.commons.config.{Configuration, ConfigurationLoader, DatasetConf, SimpleConfiguration}
import bio.ferlab.datalake.spark3.elasticsearch.{ElasticSearchClient, Indexer}
import bio.ferlab.datalake.spark3.implicits.DatasetConfImplicits.DatasetConfOperations
import software.amazon.awssdk.http.apache.ApacheHttpClient
import org.apache.spark.SparkConf
import software.amazon.awssdk.services.s3.{S3Client, S3Configuration}
import org.apache.spark.sql.functions.col
import org.apache.spark.sql.{DataFrame, SparkSession}
import software.amazon.awssdk.auth.credentials.{AwsBasicCredentials, StaticCredentialsProvider}
import pureconfig.ConfigSource
import pureconfig.generic.auto._
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.model.GetObjectRequest

import java.net.URI
//Required import
import pureconfig.module.enum._

object IndexTask extends App {

  println(s"ARGS: " + args.mkString("[", ", ", "]"))

  val Array(
  release_id,       // release id
  study_ids,        // study ids separated by ;
  jobType,          // study_centric or participant_centric or file_centric or biospecimen_centric
  env,            // qa/dev/prd
  project,        // cqdg
  esUrl,
  esPort
  ) = args

  implicit val conf: Configuration = ConfigurationLoader.loadFromResources[SimpleConfiguration](s"config/$env-$project.conf")

  val sparkConfigs: SparkConf =
    (conf.sparkconf + ("es.nodes" -> s"$esUrl:$esPort"))
      .foldLeft(new SparkConf()){ case (c, (k, v)) => c.set(k, v) }

  implicit val spark: SparkSession = SparkSession.builder
    .config(sparkConfigs)
    .enableHiveSupport()
    .appName(s"IndexTask")
    .getOrCreate()

  spark.sparkContext.setLogLevel("ERROR")

  val templatePath = s"${conf.storages.find(_.id == "storage").get.path}/templates/template_$jobType.json"

  implicit val esClient: ElasticSearchClient =
    new ElasticSearchClient(s"$esUrl:$esPort", None, None)

  val ds: DatasetConf = jobType.toLowerCase match {
    case "study_centric" => conf.getDataset("es_index_study_centric")
    case "participant_centric" => conf.getDataset("es_index_participant_centric")
    case "file_centric" => conf.getDataset("es_index_file_centric")
    case "biospecimen_centric" => conf.getDataset("es_index_biospecimen_centric")
  }

  val studyList = study_ids.split(",")

  //----------------------DELETE-----------------------------------
  spark.sparkContext.getConf.getAll
    .filterNot(c => c._1 == "spark.hadoop.fs.s3a.access.key" || c._1 =="spark.hadoop.fs.s3a.secret.key")
    .foreach(e => println(s"${e._1} -> ${e._2}"))


  val access = spark.sparkContext.getConf.getAll.filter(c => c._1 == "spark.hadoop.fs.s3a.access.key").head._2
  val secret = spark.sparkContext.getConf.getAll.filter(c => c._1 == "spark.hadoop.fs.s3a.secret.key").head._2

  val confBuilder: S3Configuration = software.amazon.awssdk.services.s3.S3Configuration.builder()
    .pathStyleAccessEnabled(true)
    .build()

  val staticCredentialsProvider: StaticCredentialsProvider = StaticCredentialsProvider.create(
    AwsBasicCredentials.create(access, secret)
  )
  val endpoint = URI.create("https://s3.ops.cqdg.ferlab.bio")

  val s3: S3Client = S3Client.builder()
    .credentialsProvider(staticCredentialsProvider)
    .endpointOverride(endpoint)
    .region(Region.US_EAST_1)
    .serviceConfiguration(confBuilder)
    .httpClient(ApacheHttpClient.create())
    .build()

  val objectRequest = GetObjectRequest
    .builder()
    .key("es_index/fhir/study_centric/study_id=ST0000017/release_id=7/part-00000-82f49241-b28c-40a2-bca8-20318ae46edd.c000.snappy.parquet")
    .bucket("cqdg-prod-app-clinical-data-service")
    .build()

  val obj = s3.getObject(objectRequest)

  println(s3.listBuckets())
  val rest = obj.response()
  println(rest)
  //---------------------------------------------------------

  studyList.foreach(studyId => {
    val indexName = s"${jobType}_${studyId}_$release_id".toLowerCase

    println(s"Run Index Task to fill index $indexName")

    println("sleep 5min")
    Thread.sleep(300000)
    println("end sleep")

    val df: DataFrame = ds.read
      .where(col("release_id") === release_id)
      .where(col("study_id") === studyId)

    new Indexer("index", templatePath, indexName)
      .run(df)
  })

}
