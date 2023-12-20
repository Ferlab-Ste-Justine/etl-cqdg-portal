package bio.ferlab.fhir.etl.config

import bio.ferlab.datalake.commons.config.Format.{AVRO, DELTA, JSON, PARQUET, VCF}
import bio.ferlab.datalake.commons.config.LoadType.{OverWrite, OverWritePartition, Read, Scd1}
import bio.ferlab.datalake.commons.config._
import bio.ferlab.datalake.commons.file.FileSystemType.S3
import bio.ferlab.datalake.spark3.genomics.GenomicDatasets
import bio.ferlab.datalake.spark3.publictables.PublicDatasets
import pureconfig.generic.auto._

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
  val storage_vcf = "storage_vcf"

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

  val sources = (
    PublicDatasets(storage, tableDatabase = Some("database"), viewDatabase = None).sources ++
      GenomicDatasets(storage, tableDatabase = Some("database"), viewDatabase = None).sources ++
      rawsAndNormalized ++ Seq(
      DatasetConf(id = "hpo_terms", storageid = storage, path = s"/hpo_terms", table = Some(TableConf("database", "hpo_terms")), format = PARQUET, loadtype = OverWrite),
      DatasetConf(id = "mondo_terms", storageid = storage, path = s"/mondo_terms", table = Some(TableConf("database", "mondo_terms")), format = PARQUET, loadtype = OverWrite),
      DatasetConf(id = "icd_terms", storageid = storage, path = s"/icd_terms", table = Some(TableConf("database", "icd_terms")), format = JSON, loadtype = OverWrite)
    ) ++ Seq(
      DatasetConf(id = "simple_participant", storageid = storage, path = s"/es_index/fhir/simple_participant", format = PARQUET, loadtype = OverWrite, partitionby = partitionByStudyIdAndReleaseId)
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
    }) ++ Seq(
      DatasetConf(
        id = "raw_vcf",
        storageid = storage_vcf,
        path = "/{{OWNER}}/{{STUDY_CODE}}/{{DATASET}}/{{BATCH}}/variants.*.vep.vcf.gz",
        format = VCF,
        loadtype = Read,
        partitionby = List("chromosome"),
        table = Some(TableConf("database", "raw_vcf")),
        keys = List("chromosome", "start", "reference", "alternate", "ensembl_transcript_id"),
        repartition = Some(RepartitionByColumns(Seq("chromosome"), Some(10)))
      ),
      DatasetConf(
        id = "normalized_snv",
        storageid = storage,
        path = s"/normalized/snv",
        format = DELTA,
        loadtype = OverWritePartition,
        table = Some(TableConf("database", "normalized_snv")),
        partitionby = List("study_id", "dataset", "batch", "has_alt", "chromosome"),
        writeoptions = WriteOptions.DEFAULT_OPTIONS ++ Map("overwriteSchema" -> "true"),
        repartition = Some(RepartitionByRange(Seq("chromosome", "start"), Some(100)))
      ),
      DatasetConf(
        id = "enriched_snv",
        storageid = storage,
        path = s"/enriched/snv",
        format = DELTA,
        loadtype = OverWritePartition,
        table = Some(TableConf("database", "enriched_snv")),
        partitionby = List("study_id", "dataset", "batch", "has_alt", "chromosome"),
        writeoptions = WriteOptions.DEFAULT_OPTIONS ++ Map("overwriteSchema" -> "true"),
        repartition = Some(RepartitionByRange(Seq("chromosome", "start"), Some(100)))
      ),
      DatasetConf(
        id = "enriched_specimen",
        storageid = storage,
        path = s"/enriched/specimen",
        format = DELTA,
        loadtype = OverWritePartition,
        table = Some(TableConf("database", "enriched_specimen")),
        partitionby = List("study_id"),
        writeoptions = WriteOptions.DEFAULT_OPTIONS ++ Map("overwriteSchema" -> "true")
      )
    ))

  val cqdgConf = Map(
    "fhir" -> "http://localhost:8080",
    "qaDbName" -> "cqdg_portal_qa",
    "prdDbName" -> "cqdg_portal_prod",
    "localDbName" -> "normalized",
    "bucketNamePrefix" -> "cqdg-{ENV}-app-datalake"
  )
  val conf = Map("cqdg" -> cqdgConf)

  val spark_conf = Map(
    "spark.databricks.delta.merge.repartitionBeforeWrite.enabled" -> "true",
    "spark.databricks.delta.retentionDurationCheck.enabled" -> "false",
    "spark.databricks.delta.schema.autoMerge.enabled" -> "true",
    "spark.delta.merge.repartitionBeforeWrite" -> "true",
    "spark.sql.autoBroadcastJoinThreshold" -> "-1",
    "spark.sql.catalog.spark_catalog" -> "org.apache.spark.sql.delta.catalog.DeltaCatalog",
    "spark.sql.extensions" -> "io.delta.sql.DeltaSparkSessionExtension",
    "spark.sql.legacy.parquet.datetimeRebaseModeInWrite" -> "CORRECTED",
    "spark.sql.legacy.timeParserPolicy" -> "CORRECTED",
    "spark.sql.mapKeyDedupPolicy" -> "LAST_WIN",
  )

  val es_conf = Map(
    "es.net.http.auth.user" -> "${?ES_USERNAME}",
    "es.net.http.auth.pass" -> "${?ES_PASSWORD}",
    "es.index.auto.create" -> "true",
    "es.net.ssl" -> "true",
    "es.net.ssl.cert.allow.self.signed" -> "true",
    "es.batch.write.retry.wait" -> "100s",
    "es.nodes.wan.only" -> "true",
    "es.wan.only" -> "true",
    "spark.es.nodes.wan.only" -> "true",
    "es.port" -> "9200"
  )
  val es_conf_local = Map(
    "es.net.ssl" -> "true",
    "es.net.ssl.keystore.location" -> "${?CERT_LOCATION}",
    "es.net.ssl.keystore.pass" -> "changeit"
  )

  conf.foreach { case (project, _) =>
    ConfigurationWriter.writeTo(s"config/output/config/dev-${project}.conf", ETLConfiguration(es_conf ++ es_conf_local, DatalakeConf(
      storages = List(
        StorageConf(storage, "s3a://cqdg-qa-app-clinical-data-service", S3),
        StorageConf(storage_vcf, "s3a://cqdg-ops-app-fhir-import-file-data", S3)
      ),
      sources = populateTable(sources, conf(project)("localDbName")),
      args = args.toList,
      sparkconf = spark_conf + ("spark.master" -> "local") + ("spark.fhir.server.url" -> conf(project)("fhir"))
    )))

    ConfigurationWriter.writeTo(s"config/output/config/qa-${project}.conf", ETLConfiguration(es_conf, DatalakeConf(
      storages = List(
        StorageConf(storage, s"s3a://${conf(project)("bucketNamePrefix").replace("{ENV}", "qa")}", S3),
        StorageConf(storage_vcf, "s3a://cqdg-qa-file-import", S3)
      ),
      sources = populateTable(sources, conf(project)("qaDbName")),
      args = args.toList,
      sparkconf = spark_conf + ("spark.fhir.server.url" -> conf(project)("fhir"))
    )))

    ConfigurationWriter.writeTo(s"config/output/config/prod-${project}.conf", ETLConfiguration(es_conf, DatalakeConf(
      storages = List(
        StorageConf(storage, s"s3a://${conf(project)("bucketNamePrefix").replace("{ENV}", "prod")}", S3),
        StorageConf(storage_vcf, "s3a://cqdg-prod-file-import", S3)
      ),
      sources = populateTable(sources, conf(project)("prdDbName")),
      args = args.toList,
      sparkconf = spark_conf + ("spark.fhir.server.url" -> conf(project)("fhir"))
    )))
  }
}
