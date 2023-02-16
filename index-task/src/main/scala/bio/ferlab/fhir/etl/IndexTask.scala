package bio.ferlab.fhir.etl

import bio.ferlab.datalake.commons.config.{Configuration, ConfigurationLoader, DatasetConf}
import bio.ferlab.datalake.spark3.elasticsearch.{ElasticSearchClient, Indexer}
import bio.ferlab.datalake.spark3.implicits.DatasetConfImplicits.DatasetConfOperations
import org.apache.spark.SparkConf
import org.apache.spark.sql.functions.col
import org.apache.spark.sql.{DataFrame, SparkSession}
import pureconfig._
import pureconfig.generic.auto._

case class ServiceConf(
                        esHost: String,
                        esUsername: Option[String],
                        esPassword: Option[String]
                      )


object IndexTask extends App {

  println(s"ARGS: " + args.mkString("[", ", ", "]"))

  val Array(
  release_id,       // release id
  study_ids,        // study ids separated by ;
  jobType,          // study_centric or participant_centric or file_centric or biospecimen_centric
  env,            // qa/dev/prd
  project        // cqdg
  ) = args

  implicit val conf: Configuration = ConfigurationLoader.loadFromResources(s"config/$env-$project.conf")
  val indexConf: ServiceConf = ConfigSource.file(s"index-task/src/main/resources/$env.conf").loadOrThrow[ServiceConf]

  val esConfigs = Map(
    "es.index.auto.create" -> "true",
    "es.nodes"-> indexConf.esHost,
    "es.port"-> "443",
    "es.batch.write.retry.wait" -> "100s",
    "es.nodes.client.only" -> "false",
    "es.nodes.discovery" -> "false",
    "es.nodes.wan.only" -> "true",
    "es.read.ignore_exception" -> "true",
    "es.wan.only" -> "true",
    "es.write.ignore_exception" -> "true",
//    "es.net.ssl" -> "true",
//    "es.net.ssl.keystore.location" -> "file:///home/adrian/.jdks/corretto-11.0.13/lib/security/cacerts",
//    "es.net.ssl.keystore.pass" -> "changeit"
  )

  val esUserPass = (indexConf.esUsername, indexConf.esPassword) match {
    case (Some(u), Some(p)) => Map("es.net.http.auth.user"-> u, "es.net.http.auth.pass" -> p)
    case _ => Map.empty[String, String]
  }

  val sparkConfigs: SparkConf =
    (conf.sparkconf ++ esConfigs ++ esUserPass)
      .foldLeft(new SparkConf()){ case (c, (k, v)) => c.set(k, v) }

  implicit val spark: SparkSession = SparkSession.builder
    .config(sparkConfigs)
    .enableHiveSupport()
    .appName(s"IndexTask")
    .getOrCreate()

  spark.sparkContext.setLogLevel("ERROR")

  val templatePath = s"${conf.storages.find(_.id == "storage").get.path}/templates/template_$jobType.json"

  implicit val esClient: ElasticSearchClient = new ElasticSearchClient(indexConf.esHost, indexConf.esUsername, indexConf.esPassword)

  val ds: DatasetConf = jobType.toLowerCase match {
    case "study_centric" => conf.getDataset("es_index_study_centric")
    case "participant_centric" => conf.getDataset("es_index_participant_centric")
    case "file_centric" => conf.getDataset("es_index_file_centric")
    case "biospecimen_centric" => conf.getDataset("es_index_biospecimen_centric")
  }

  val studyList = study_ids.split(",")

  studyList.foreach(studyId => {
    val indexName = s"${jobType}_${studyId}_$release_id".toLowerCase

    println(s"Run Index Task to fill index $indexName")

    val df: DataFrame = ds.read
      .where(col("release_id") === release_id)
      .where(col("study_id") === studyId)

    new Indexer("index", templatePath, indexName)
      .run(df)
  })

}
