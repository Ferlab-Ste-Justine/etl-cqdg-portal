package bio.ferlab.fhir.etl

import bio.ferlab.datalake.commons.config.{Configuration, ConfigurationLoader, ConfigurationWrapper, DatalakeConf, DatasetConf, SimpleConfiguration}
import bio.ferlab.datalake.spark3.elasticsearch.{ElasticSearchClient, Indexer}
import bio.ferlab.datalake.spark3.implicits.DatasetConfImplicits.DatasetConfOperations
import org.apache.spark.SparkConf
import org.apache.spark.sql.functions.col
import org.apache.spark.sql.{DataFrame, SparkSession}
import pureconfig.ConfigSource
import pureconfig.generic.auto._
//Required import
import pureconfig.module.enum._

case class ServiceConf(esConfig: Map[String, String])


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
  val serviceConf: ServiceConf = ConfigSource.resources(s"config/$env-$project.conf").loadOrThrow[ServiceConf]
  private val esConf = serviceConf.esConfig

  val sparkConfigs: SparkConf =
    (conf.sparkconf ++ esConf + ("es.nodes" -> s"$esUrl:$esPort"))
      .foldLeft(new SparkConf()){ case (c, (k, v)) => c.set(k, v) }

  implicit val spark: SparkSession = SparkSession.builder
    .config(sparkConfigs)
    .enableHiveSupport()
    .appName(s"IndexTask")
    .getOrCreate()

  spark.sparkContext.setLogLevel("ERROR")

  val templatePath = s"${conf.storages.find(_.id == "storage").get.path}/templates/template_$jobType.json"

  implicit val esClient: ElasticSearchClient =
    new ElasticSearchClient(s"$esUrl:$esPort", esConf.get("es.net.http.auth.user"), esConf.get("es.net.http.auth.pass"))

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

//    df.show(2, false)

    println(s"$esUrl:$esPort")
    println(s"env: $env")
    println(esConf.get("es.net.http.auth.user"))
    println(esConf.get("es.net.http.auth.pass"))

    (esConf + ("es.nodes" -> s"$esUrl:$esPort")).foreach(println)

    new Indexer("index", templatePath, indexName)
      .run(df)
  })

}
