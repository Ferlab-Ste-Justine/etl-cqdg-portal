package bio.ferlab.fhir.etl

import bio.ferlab.datalake.commons.config.Format.{AVRO, DELTA}
import bio.ferlab.datalake.commons.config.LoadType.{OverWrite, OverWritePartition}
import bio.ferlab.datalake.commons.config.{Configuration, ConfigurationWrapper, DatalakeConf, DatasetConf, SimpleConfiguration, StorageConf, TableConf}
import bio.ferlab.datalake.commons.file.FileSystemType.S3
import bio.ferlab.fhir.etl
import bio.ferlab.fhir.etl.ImportTask.{jobs, releaseId}
import bio.ferlab.fhir.etl.fhavro.FhavroToNormalizedMappings
import bio.ferlab.fhir.etl.fhavro.FhavroToNormalizedMappings.mappings
import org.apache.spark.sql.functions.col
import org.apache.spark.sql.internal.SQLConf.LegacyBehaviorPolicy.CORRECTED
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.apache.spark.sql.SparkSession

case class ETLConfiguration(`es-config`: Map[String, String], datalake: DatalakeConf) extends ConfigurationWrapper(datalake)

class FhavroToNormalizedMappingsSpec
  extends AnyFlatSpec
    with Matchers{

  val storage = "storage"

  implicit val spark: SparkSession = SparkSession.builder()
    .config("spark.ui.enabled", value = false)
    .config("spark.hadoop.fs.s3a.endpoint", "http://localhost:9000")
    .config("spark.databricks.delta.merge.repartitionBeforeWrite.enabled", "true")
    .config("spark.databricks.delta.retentionDurationCheck.enabled", "false")
    .config("spark.databricks.delta.schema.autoMerge.enabled", "true")
    .config("spark.delta.merge.repartitionBeforeWrite", "true")
    .config("spark.fhir.server.url", "http://localhost:8080")
    .config("spark.sql.autoBroadcastJoinThreshold", "-1")
    .config("spark.sql.catalog.spark_catalog", "org.apache.spark.sql.delta.catalog.DeltaCatalog")
    .config("spark.sql.extensions", "io.delta.sql.DeltaSparkSessionExtension")
    .config("spark.sql.legacy.parquet.datetimeRebaseModeInWrite", "CORRECTED")
    .config("spark.sql.legacy.timeParserPolicy", "CORRECTED")
    .config("spark.sql.mapKeyDedupPolicy", "LAST_WIN")
    .config("fs.s3a.path.style.access", "true")
    .master("local")
    .getOrCreate()

  spark.sparkContext.setLogLevel("ERROR")


  "method mappings" should "test" in {
    val storages = List(
      StorageConf(filesystem = S3, id = "storage", path = "s3a://cqdg-qa-app-clinical-data-service")
    )

    val sources = List(
//      DatasetConf(
//        format = AVRO,
//        id = "raw_sample_registration",
//        keys = List.empty,
//        loadtype = OverWrite,
//        partitionby = List("study_id","release_id"),
//        path = "/fhir/specimen",
//        readoptions = Map.empty,
//        storageid = storage,
//        writeoptions = Map(
//          "created_on_column" -> "created_on",
//          "is_current_column" -> "is_current",
//          "updated_on_column" -> "updated_on",
//          "valid_from_column" -> "valid_from",
//          "valid_to_column" -> "valid_to",
//        ),
//      ),
//      DatasetConf(
//        format = DELTA,
//        id = "normalized_sample_registration",
//        keys = List.empty,
//        loadtype = OverWritePartition,
//        partitionby = List("study_id","release_id"),
//        path = "/normalized/sample_registration",
//        readoptions = Map.empty,
//        storageid = storage,
//        writeoptions = Map(
//          "created_on_column" -> "created_on",
//          "is_current_column" -> "is_current",
//          "overwriteSchema" -> "true",
//          "updated_on_column" -> "updated_on",
//          "valid_from_column" -> "valid_from",
//          "valid_to_column" -> "valid_to",
//        ),
//      ),
      DatasetConf(
        format = AVRO,
        id = "raw_biospecimen",
        keys = List.empty,
        loadtype = OverWrite,
        partitionby = List("study_id", "release_id"),
        path = "/fhir/specimen",
        readoptions = Map.empty,
        storageid = storage,
        writeoptions = Map(
          "created_on_column" -> "created_on",
          "is_current_column" -> "is_current",
          "updated_on_column" -> "updated_on",
          "valid_from_column" -> "valid_from",
          "valid_to_column" -> "valid_to",
        ),
      ),
      DatasetConf(
        format = DELTA,
        id = "normalized_biospecimen",
        keys = List.empty,
        loadtype = OverWritePartition,
        partitionby = List("study_id", "release_id"),
        path = "/normalized/biospecimen",
        readoptions = Map.empty,
        storageid = storage,
        writeoptions = Map(
          "created_on_column" -> "created_on",
          "is_current_column" -> "is_current",
          "overwriteSchema" -> "true",
          "updated_on_column" -> "updated_on",
          "valid_from_column" -> "valid_from",
          "valid_to_column" -> "valid_to",
        ),
      ),
      DatasetConf(
        format = AVRO,
        id = "raw_patient",
        keys = List.empty,
        loadtype = OverWrite,
        partitionby = List("study_id", "release_id"),
        path = "/fhir/patient",
        readoptions = Map.empty,
        storageid = storage,
        writeoptions = Map(
          "created_on_column" -> "created_on",
          "is_current_column" -> "is_current",
          "updated_on_column" -> "updated_on",
          "valid_from_column" -> "valid_from",
          "valid_to_column" -> "valid_to",
        ),
      ),
      DatasetConf(
        format = DELTA,
        id = "normalized_patient",
        keys = List.empty,
        loadtype = OverWritePartition,
        partitionby = List("study_id", "release_id"),
        path = "/normalized/patient",
        readoptions = Map.empty,
        storageid = storage,
        writeoptions = Map(
          "created_on_column" -> "created_on",
          "is_current_column" -> "is_current",
          "overwriteSchema" -> "true",
          "updated_on_column" -> "updated_on",
          "valid_from_column" -> "valid_from",
          "valid_to_column" -> "valid_to",
        ),
      ),
      DatasetConf(
        format = AVRO,
        id = "raw_diagnosis",
        keys = List.empty,
        loadtype = OverWrite,
        partitionby = List("study_id", "release_id"),
        path = "/fhir/condition",
        readoptions = Map.empty,
        storageid = storage,
        writeoptions = Map(
          "created_on_column" -> "created_on",
          "is_current_column" -> "is_current",
          "updated_on_column" -> "updated_on",
          "valid_from_column" -> "valid_from",
          "valid_to_column" -> "valid_to",
        ),
      ),
      DatasetConf(
        format = DELTA,
        id = "normalized_diagnosis",
        keys = List.empty,
        loadtype = OverWritePartition,
        partitionby = List("study_id", "release_id"),
        path = "/normalized/diagnosis",
        readoptions = Map.empty,
        storageid = storage,
        writeoptions = Map(
          "created_on_column" -> "created_on",
          "is_current_column" -> "is_current",
          "overwriteSchema" -> "true",
          "updated_on_column" -> "updated_on",
          "valid_from_column" -> "valid_from",
          "valid_to_column" -> "valid_to",
        ),
      )
    )
    val sparkConfWithExcludeCollectionEntry = Map(
      "spark.master" -> "local[*]",
      "spark.databricks.delta.retentionDurationCheck.enabled" -> "false",
      "spark.delta.merge.repartitionBeforeWrite" -> "true",
      "spark.fhir.server.url" -> "http://localhost:8080",
      "spark.hadoop.fs.s3a.endpoint" -> "http://localhost:9022",
      "fs.s3a.endpoint" -> "http://localhost:9022",
      "spark.sql.catalog.spark_catalog" -> "org.apache.spark.sql.delta.catalog.DeltaCatalog",
      "spark.sql.extensions" -> "io.delta.sql.DeltaSparkSessionExtension",
      "spark.sql.legacy.parquet.datetimeRebaseModeInWrite" -> CORRECTED.toString,
      "spark.sql.legacy.timeParserPolicy" -> CORRECTED.toString,
      "spark.sql.mapKeyDedupPolicy" -> "LAST_WIN",
    )
    implicit val c1 = ETLConfiguration(
      Map.empty[String, String],
      datalake = DatalakeConf(
        storages,
        sources,
        List.empty,
        sparkConfWithExcludeCollectionEntry
      )
    )
    val jobs = FhavroToNormalizedMappings
      .mappings("8")
      .map { case (src, dst, transformations) => new ImportRawToNormalizedETL(src, dst, transformations, "8", List("ST0000017")) }

    jobs.foreach(_.run().foreach(e => println(e._2.count())))
    jobs.foreach(_.run().foreach(_._2.show(false)))



    1 shouldBe(1)
  }
}
