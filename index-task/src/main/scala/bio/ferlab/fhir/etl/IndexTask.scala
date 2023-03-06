package bio.ferlab.fhir.etl

import bio.ferlab.datalake.commons.config.{Configuration, ConfigurationLoader, DatasetConf, SimpleConfiguration}
import bio.ferlab.datalake.spark3.elasticsearch.{ElasticSearchClient, Indexer}
import bio.ferlab.datalake.spark3.implicits.DatasetConfImplicits.DatasetConfOperations
import org.apache.hadoop.shaded.org.apache.http.util.EntityUtils
import org.apache.spark.SparkConf
import org.apache.spark.sql.functions.col
import org.apache.spark.sql.{DataFrame, SparkSession}
import pureconfig.generic.auto._
import org.elasticsearch.spark.sql._
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
  esPort,
  test
  ) = args

  val esConfigs = Map(
    "es.index.auto.create" -> "true",
    "es.net.ssl" -> "false",
    "es.net.ssl.cert.allow.self.signed" -> "true",
    "es.nodes" -> s"$esUrl", //https://elasticsearch-workers:9200
    "es.nodes.wan.only" -> "true",
    "es.wan.only" -> "true",
    "spark.es.nodes.wan.only" -> "true",
    "es.port" -> esPort) //9200

  println(esUrl)
  println(esPort)

  implicit val conf: Configuration = ConfigurationLoader.loadFromResources[SimpleConfiguration](s"config/$env-$project.conf")

  val sparkConfigs: SparkConf =
//    (conf.sparkconf + ("es.nodes" -> s"$esUrl:$esPort"))
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
    new ElasticSearchClient(s"$esUrl:$esPort", None, None)

  spark.sparkContext.getConf.getAll.filterNot(e => e._1 == "spark.hadoop.fs.s3a.access.key" || e._1 == "spark.hadoop.fs.s3a.secret.key")
    .foreach(r => println(s"${r._1} -> ${r._2}"))

  if(test.toLowerCase() == "true"){
    println("toto")
    println(EntityUtils.toString(esClient.getIndex("arranger-projects").getEntity))
    Thread.sleep(300000)
    println("toto")
  }

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

    df.saveToEs("test/_doc", Map("es.write.operation"-> "index"))

//    new Indexer("index", templatePath, indexName)
//      .run(df)
  })

}
