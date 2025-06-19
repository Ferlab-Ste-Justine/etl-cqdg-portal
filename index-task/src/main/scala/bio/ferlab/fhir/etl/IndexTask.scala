package bio.ferlab.fhir.etl

import bio.ferlab.datalake.commons.config.{Configuration, ConfigurationLoader, DatasetConf, SimpleConfiguration}
import bio.ferlab.datalake.spark3.elasticsearch.ElasticSearchClient
import bio.ferlab.datalake.spark3.implicits.DatasetConfImplicits.DatasetConfOperations
import org.apache.spark.SparkConf
import org.apache.spark.sql.functions.col
import org.apache.spark.sql.{DataFrame, SparkSession}
import pureconfig.ConfigSource
import pureconfig.generic.auto._

object IndexTask extends App {

  println(s"ARGS: " + args.mkString("[", ", ", "]"))

  val Array(
  release_id,       // release id
  study_ids,        // study ids separated by ;
  jobType,          // study_centric or participant_centric or file_centric or biospecimen_centric or program_centric
  env,            // qa/dev/prd
  project,        // cqdg
  esUrl,
  esPort
  ) = args

  private val esUsername = sys.env.get("ES_USERNAME")
  private val esPassword = sys.env.get("ES_PASSWORD")

  private val defaultEsConfigs = Map(
    "spark.master" -> "local[*]",
    "es.index.auto.create" -> "true",
    "opensearch.index.auto.create" -> "true",
    "es.net.ssl" -> "true",
    "opensearch.net.ssl" -> "true",
    "es.net.ssl.cert.allow.self.signed" -> "true",
    "opensearch.net.ssl.cert.allow.self.signed" -> "true",
    "es.nodes" -> s"$esUrl", //https://elasticsearch-workers:9200
    "opensearch.nodes" -> s"$esUrl", //https://elasticsearch-workers:9200
    "es.nodes.wan.only" -> "true",
    "opensearch.nodes.wan.only" -> "true",
    "es.wan.only" -> "true",
    "opensearch.wan.only" -> "true",
    "spark.es.nodes.wan.only" -> "true",
    "es.port" -> esPort,
    "opensearch.port" -> esPort) //9200

  private val esConfigs = (esUsername, esPassword) match {
    case (Some(u), Some(p)) => defaultEsConfigs ++
      Map(
        "es.net.http.auth.user" -> u,
        "es.net.http.auth.pass" -> p,
        "opensearch.net.http.auth.user" -> u,
        "opensearch.net.http.auth.pass" -> p
      )
    case _ => defaultEsConfigs
  }

  implicit val conf: Configuration = ConfigurationLoader.loadFromResources[SimpleConfiguration](s"config/$env-$project.conf")

  val sparkConfigs: SparkConf =
    (conf.sparkconf ++ esConfigs)
      .foldLeft(new SparkConf()){ case (c, (k, v)) => c.set(k, v) }

  implicit val spark: SparkSession = SparkSession.builder
    .config(sparkConfigs)
    .enableHiveSupport()
    .appName(s"IndexTask")
    .getOrCreate()

  spark.sparkContext.setLogLevel("ERROR")

  val templatePath = s"${conf.storages.find(_.id == "storage").get.path}/templates/template_$jobType.json"

  implicit val esClient: ElasticSearchClient =
    new ElasticSearchClient(s"$esUrl:$esPort", esUsername, esPassword)

  jobType.toLowerCase match {
    case "program_centric" =>
      println(s"Run Index Task to fill index program_centric_$release_id")
      // Program is unic, not per study
      val programDf: DataFrame = conf.getDataset(s"es_index_program_centric").read
      new Indexer("index", templatePath, s"program_centric_$release_id").run(programDf)

    case _ =>
      val ds: DatasetConf = conf.getDataset(s"es_index_${jobType.toLowerCase}")

      val studyList = study_ids.split(",")

      studyList.foreach(studyId => {
        val indexName = s"${jobType}_${studyId}_$release_id".toLowerCase

        println(s"Run Index Task to fill index $indexName")

        val df: DataFrame = ds.read
          .where(col("study_id") === studyId)

        new Indexer("index", templatePath, indexName)
          .run(df)
      })
  }
}
