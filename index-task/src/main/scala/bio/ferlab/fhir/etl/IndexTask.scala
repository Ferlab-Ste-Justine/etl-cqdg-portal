package bio.ferlab.fhir.etl

import bio.ferlab.datalake.commons.config.{Configuration, ConfigurationLoader, DatasetConf, SimpleConfiguration}
import bio.ferlab.datalake.spark3.elasticsearch.{ElasticSearchClient, Indexer}
import bio.ferlab.datalake.spark3.implicits.DatasetConfImplicits.DatasetConfOperations
import org.apache.spark.SparkConf
import org.apache.spark.sql.functions.{col, struct}
import org.apache.spark.sql.{DataFrame, SparkSession}
import pureconfig.generic.auto._
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

  val esConfigs = Map(
    "spark.master" -> "local[*]",
    "es.index.auto.create" -> "true",
    "es.net.ssl" -> "true",
    "es.net.ssl.cert.allow.self.signed" -> "true",
    "es.nodes" -> s"$esUrl", //https://elasticsearch-workers:9200
    "es.nodes.wan.only" -> "true",
    "es.wan.only" -> "true",
    "spark.es.nodes.wan.only" -> "true",
    "es.port" -> esPort) //9200

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
    new ElasticSearchClient(s"$esUrl:$esPort", None, None)

  spark.sparkContext.getConf.getAll.filterNot(e => e._1 == "spark.hadoop.fs.s3a.access.key" || e._1 == "spark.hadoop.fs.s3a.secret.key")
    .foreach(r => println(s"${r._1} -> ${r._2}"))

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

    if(jobType == "participant_centric") {
      val suggestionsDF = df
        .select("study", "observed_phenotype_tagged", "icd_tagged", "mondo_tagged", "gender", "ethnicity", "biospecimens", "files")
        .withColumn("study_slim", struct(col("study")("study_code") as "study_code"))
        .drop("study").withColumnRenamed("study_slim", "study")
        .withColumn("observed_phenotype_tagged_slim", struct(
          col("observed_phenotype_tagged")("name") as "name",
          col("observed_phenotype_tagged")("source_text") as "source_text",
        ))
        .drop("observed_phenotype_tagged").withColumnRenamed("observed_phenotype_tagged_slim", "observed_phenotype_tagged")
        .withColumn("icd_tagged_slim", struct(col("icd_tagged")("name") as "name"))
        .drop("icd_tagged").withColumnRenamed("icd_tagged_slim", "icd_tagged")
        .withColumn("mondo_tagged_slim", struct(
          col("mondo_tagged")("name") as "name",
          col("mondo_tagged")("source_text") as "source_text",
        ))
        .drop("mondo_tagged").withColumnRenamed("mondo_tagged_slim", "mondo_tagged")
        .withColumn("biospecimens_slim", struct(
          col("biospecimens")("biospecimen_id") as "biospecimen_id",
          col("biospecimens")("biospecimen_tissue_source") as "biospecimen_tissue_source",
          col("biospecimens")("sample_type") as "sample_type",
        ))
        .drop("biospecimens").withColumnRenamed("biospecimens_slim", "biospecimens")
        .withColumn("biospecimens_slim", struct(
          col("biospecimens")("biospecimen_id") as "biospecimen_id",
          col("biospecimens")("biospecimen_tissue_source") as "biospecimen_tissue_source",
          col("biospecimens")("sample_type") as "sample_type",
        ))
        .drop("biospecimens").withColumnRenamed("biospecimens_slim", "biospecimens")
        .withColumn("files_slim", struct(
          col("files")("file_id") as "file_id",
          col("files")("data_type") as "data_type",
          col("files")("data_category") as "data_category",
          col("files")("file_format") as "file_format",
          struct(col("sequencing_experiment")("experimental_strategy") as "experimental_strategy") as "sequencing_experiment"
        ))
        .drop("files").withColumnRenamed("files_slim", "files")

      new Indexer("index", s"${conf.storages.find(_.id == "storage").get.path}/templates/template_participant_suggestions.json", s"participant_suggestions_${studyId}_$release_id".toLowerCase)
        .run(suggestionsDF)
    }

    new Indexer("index", templatePath, indexName)
      .run(df)
  })

}
