package bio.ferlab.fhir.etl.config

import bio.ferlab.datalake.commons.config.Format.{AVRO, CSV, DELTA, JSON, PARQUET}
import bio.ferlab.datalake.commons.config.LoadType.{OverWrite, OverWritePartition}
import bio.ferlab.datalake.commons.config._
import bio.ferlab.datalake.commons.file.FileSystemType.S3

case class SourceConfig(fhirResource: String, entityType: Option[String], partitionBy: List[String])

case class Index(name: String, partitionBy: List[String])

object ConfigurationGenerator extends App {
  def populateTable(sources: List[DatasetConf], tableName: String): List[DatasetConf] = {
    sources.map(ds => ds.copy(table = ds.table.map(t => TableConf(tableName, t.name))))
  }

  private val partitionByStudyIdAndReleaseId = List("study_id", "release_id")
  val sourceNames: Seq[SourceConfig] = Seq(
    SourceConfig("patient", None, partitionByStudyIdAndReleaseId),
    SourceConfig("condition", Some("diagnosis"), partitionByStudyIdAndReleaseId),
    SourceConfig("observation", Some("cause_of_death"), partitionByStudyIdAndReleaseId),
    SourceConfig("observation", Some("disease_status"), partitionByStudyIdAndReleaseId),
    SourceConfig("observation", Some("phenotype"), partitionByStudyIdAndReleaseId),
    SourceConfig("specimen", Some("biospecimen"), partitionByStudyIdAndReleaseId),
    SourceConfig("specimen", Some("sample_registration"), partitionByStudyIdAndReleaseId),
    SourceConfig("researchstudy", Some("research_study"), partitionByStudyIdAndReleaseId),
    SourceConfig("documentreference", Some("document_reference"), partitionByStudyIdAndReleaseId),
    SourceConfig("observation", Some("family_relationship"), partitionByStudyIdAndReleaseId),
    SourceConfig("observation", Some("tumor_normal_designation"), partitionByStudyIdAndReleaseId),
    SourceConfig("group", None, partitionByStudyIdAndReleaseId),
    SourceConfig("task", None, partitionByStudyIdAndReleaseId)
  )

  val storage = "storage"

  val rawsAndNormalized = sourceNames.flatMap(source => {
    val rawPath = source.fhirResource
    val tableName = source.entityType.map(_.replace("-", "_")).getOrElse(source.fhirResource.replace("-", "_"))
    Seq(
      DatasetConf(
        id = s"raw_$tableName",
        storageid = storage,
        path = s"/fhir/$rawPath",
        format = AVRO,
        loadtype = OverWrite,
        table = Some(TableConf("database", s"raw_$tableName")),
        partitionby = source.partitionBy
      ),
      DatasetConf(
        id = s"normalized_$tableName",
        storageid = storage,
        path = s"/normalized/${source.entityType.getOrElse(source.fhirResource)}",
        format = DELTA,
        loadtype = OverWritePartition,
        table = Some(TableConf("database", tableName)),
        partitionby = source.partitionBy,
        writeoptions = WriteOptions.DEFAULT_OPTIONS ++ Map("overwriteSchema" -> "true")
      )
    )
  })

  val sources = (rawsAndNormalized ++ Seq(
    DatasetConf(
      id = "hpo_terms",
      storageid = storage,
      path = s"/hpo_terms",
      format = JSON,
      table = Some(TableConf("database", "hpo_terms")),
      loadtype = OverWrite,
    ),
    DatasetConf(
      id = "mondo_terms",
      storageid = storage,
      path = s"/mondo_terms",
      table = Some(TableConf("database", "mondo_terms")),
      format = JSON,
      loadtype = OverWrite,
    ),
    DatasetConf(
      id = "duo_terms", //TODO should be removed
      storageid = storage,
      path = s"/duo_terms",
      table = Some(TableConf("database", "duo_terms")),
      readoptions = Map("header" -> "true"),
      format = CSV,
      loadtype = OverWrite,
    ),
    DatasetConf(
      id = "icd_terms",
      storageid = storage,
      path = s"/icd_terms",
      table = Some(TableConf("database", "icd_terms")),
      format = JSON,
      loadtype = OverWrite,
    )
  ) ++ Seq(
    DatasetConf(
      id = "simple_participant",
      storageid = storage,
      path = s"/es_index/fhir/simple_participant",
      format = PARQUET,
      loadtype = OverWrite,
      partitionby = partitionByStudyIdAndReleaseId
    )
  ) ++ Seq(
    Index("study_centric", partitionByStudyIdAndReleaseId),
    Index("participant_centric", partitionByStudyIdAndReleaseId),
    Index("file_centric", partitionByStudyIdAndReleaseId),
    Index("biospecimen_centric", partitionByStudyIdAndReleaseId),
  ).flatMap(index => {
    Seq(
      DatasetConf(
        id = s"es_index_${index.name}",
        storageid = storage,
        path = s"/es_index/fhir/${index.name}",
        format = PARQUET,
        loadtype = OverWrite,
        table = Some(TableConf("database", s"es_index_${index.name}")),
        partitionby = index.partitionBy
      )
    )
  })).toList

  val cqdgConf = Map("fhir" -> "http://localhost:8080", "qaDbName" -> "cqdg_portal_qa", "prdDbName" -> "cqdg_portal_prd", "localDbName" -> "normalized", "bucketNamePrefix" -> "clinical-data/e2adb961-4f58-4e13-a24f-6725df802e2c")
  val conf = Map("cqdg" -> cqdgConf)

  conf.foreach { case (project, _) =>
    val spark_conf = Map(
      "spark.databricks.delta.retentionDurationCheck.enabled" -> "false",
      "spark.delta.merge.repartitionBeforeWrite" -> "true",
      "spark.sql.catalog.spark_catalog" -> "org.apache.spark.sql.delta.catalog.DeltaCatalog",
      "spark.sql.extensions" -> "io.delta.sql.DeltaSparkSessionExtension",
      "spark.sql.legacy.parquet.datetimeRebaseModeInWrite" -> "CORRECTED",
      "spark.sql.legacy.timeParserPolicy" -> "CORRECTED",
      "spark.sql.mapKeyDedupPolicy" -> "LAST_WIN",
      "spark.fhir.server.url" -> conf(project)("fhir")
    )

    ConfigurationWriter.writeTo(s"config/output/config/dev-${project}.conf", Configuration(
      storages = List(
        StorageConf(storage, "s3a://cqdg-qa-app-clinical-data-service", S3)
      ),
      sources = populateTable(sources, conf(project)("localDbName")),
      args = args.toList,
      sparkconf = Map(
        "spark.databricks.delta.retentionDurationCheck.enabled" -> "false",
        "spark.delta.merge.repartitionBeforeWrite" -> "true",
        "spark.hadoop.fs.s3a.access.key" -> "${?AWS_ACCESS_KEY}",
        "spark.hadoop.fs.s3a.endpoint" -> "${?AWS_ENDPOINT}",
        "spark.hadoop.fs.s3a.impl" -> "org.apache.hadoop.fs.s3a.S3AFileSystem",
        "spark.hadoop.fs.s3a.path.style.access" -> "true",
        "spark.hadoop.fs.s3a.secret.key" -> "${?AWS_SECRET_KEY}",
        "spark.master" -> "local",
        "spark.sql.catalog.spark_catalog" -> "org.apache.spark.sql.delta.catalog.DeltaCatalog",
        "spark.sql.extensions" -> "io.delta.sql.DeltaSparkSessionExtension",
        "spark.sql.legacy.timeParserPolicy" -> "CORRECTED",
        "spark.sql.mapKeyDedupPolicy" -> "LAST_WIN",
        "spark.fhir.server.url" -> conf(project)("fhir")
      )
    ))

    ConfigurationWriter.writeTo(s"config/output/config/qa-${project}.conf", Configuration(
      storages = List(
        StorageConf(storage, s"s3a://${conf(project)("bucketNamePrefix")}-qa", S3)
      ),
      sources = populateTable(sources, conf(project)("qaDbName")),
      args = args.toList,
      sparkconf = spark_conf
    ))

    ConfigurationWriter.writeTo(s"config/output/config/prd-${project}.conf", Configuration(
      storages = List(
        StorageConf(storage, s"s3a://${conf(project)("bucketNamePrefix")}-prd", S3)
      ),
      sources = populateTable(sources, conf(project)("prdDbName")),
      args = args.toList,
      sparkconf = spark_conf
    ))
  }
}
