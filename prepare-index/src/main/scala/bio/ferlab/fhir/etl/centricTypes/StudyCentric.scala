package bio.ferlab.fhir.etl.centricTypes

import bio.ferlab.datalake.commons.config.{Configuration, DatasetConf}
import bio.ferlab.datalake.spark3.etl.v2.ETL
import bio.ferlab.datalake.spark3.implicits.DatasetConfImplicits.DatasetConfOperations
import org.apache.spark.sql.{DataFrame, SparkSession}
import bio.ferlab.fhir.etl.common.Utils._
import org.apache.spark.sql.functions.{col, transform => sparkTransform, _}

import java.time.LocalDateTime

class StudyCentric(studyIds: List[String])(implicit configuration: Configuration) extends ETL {

  override val mainDestination: DatasetConf = conf.getDataset("es_index_study_centric")
  val normalized_researchstudy: DatasetConf = conf.getDataset("normalized_research_study")
  val normalized_drs_document_reference: DatasetConf = conf.getDataset("normalized_document_reference")
  val normalized_patient: DatasetConf = conf.getDataset("normalized_patient")
  val normalized_group: DatasetConf = conf.getDataset("normalized_group")
  val normalized_diagnosis: DatasetConf = conf.getDataset("normalized_diagnosis")
  val normalized_task: DatasetConf = conf.getDataset("normalized_task")
  val normalized_biospecimen: DatasetConf = conf.getDataset("normalized_biospecimen")
  val normalized_phenotype: DatasetConf = conf.getDataset("normalized_phenotype")
  val normalized_sequencing_experiment: DatasetConf = conf.getDataset("normalized_task")
  val normalized_sample_registration: DatasetConf = conf.getDataset("normalized_sample_registration")

  override def extract(lastRunDateTime: LocalDateTime = minDateTime,
                       currentRunDateTime: LocalDateTime = LocalDateTime.now())(implicit spark: SparkSession): Map[String, DataFrame] = {
    Seq(
      normalized_researchstudy, normalized_drs_document_reference, normalized_patient, normalized_group,
      normalized_diagnosis, normalized_task, normalized_phenotype, normalized_sequencing_experiment, normalized_biospecimen,
      normalized_sample_registration)
      .map(ds => ds.id -> ds.read.where(col("study_id").isin(studyIds: _*))).toMap

  }

  override def transform(data: Map[String, DataFrame],
                         lastRunDateTime: LocalDateTime = minDateTime,
                         currentRunDateTime: LocalDateTime = LocalDateTime.now())(implicit spark: SparkSession): Map[String, DataFrame] = {

    val studyDF = data(normalized_researchstudy.id)

    val samplesCount =
      data(normalized_patient.id)
        .select("fhir_id", "study_id")
        .withColumnRenamed("fhir_id", "participant_id")
        .addFilesWithBiospecimen(
          data(normalized_drs_document_reference.id),
          data(normalized_biospecimen.id),
          data(normalized_sequencing_experiment.id),
          data(normalized_sample_registration.id),
        )
        .withColumn("file_exp", explode(col("files")))
        .withColumn("bio_exp", explode(col("file_exp.biospecimens")))
        .select("study_id", "bio_exp.sample_id")
        .distinct().filter(col("sample_id").isNotNull)
        .groupBy("study_id")
        .agg(size(collect_list("sample_id")) as "sample_count")

    val participantsWithFiles = data(normalized_patient.id)
      .withColumnRenamed("fhir_id", "subject")
      .addFiles(data(normalized_drs_document_reference.id), data(normalized_sequencing_experiment.id))

    val dataTypesCount = participantsWithFiles
      .fieldCount("data_type", "data_types")

    val dataCategoryCount = participantsWithFiles
      .fieldCount("data_category", "data_categories_from_files")

    val participantCount =
      participantsWithFiles
        .groupBy("study_id")
        .agg(size(collect_set("subject")) as "participant_count")

    val participantsRenamed = data(normalized_patient.id).withColumnRenamed("fhir_id", "participant_id")

    val fileCount =
      data(normalized_drs_document_reference.id)
//        .addAssociatedDocumentRef()
        .filter(!col("relates_to").isNotNull).drop("relates_to")
        .withColumn("files_exp", explode(col("files")))
        .select("*", "files_exp.*")
        .drop("files_exp", "files")

        .join(participantsRenamed, Seq("participant_id", "study_id"), "inner")
        .groupBy("study_id")
        .agg(
          size(collect_set(
            struct(
              col("file_name"),
              col("file_format"),
              col("fhir_id"))
          )) as "file_count",
          collect_set(col("data_category")) as "data_category"
        )

    val familyCount = data(normalized_group.id)
      .withColumn("subject", explode(col("family_members")))
      .join(participantsWithFiles, Seq("study_id", "subject"), "inner")
      .groupBy("study_id", "internal_family_id", "submitter_family_id")
      .agg(collect_set(col("subject")) as "family_members")
      .where(size(col("family_members")).gt(1))
      .groupBy("study_id")
      .agg(size(collect_set(col("internal_family_id"))) as "family_count")

    val studyDatasets = studyDF
      .addDataSetToStudy(data(normalized_drs_document_reference.id), participantsRenamed, data(normalized_sequencing_experiment.id))

    val combinedDataCategories = studyDF.combineDataCategoryFromFilesAndStudy(dataCategoryCount)

    val transformedStudyDf = studyDF
      .join(studyDatasets, Seq("study_id"), "left_outer")
      .join(samplesCount, Seq("study_id"), "left_outer")
      .drop("data_categories")
      .join(dataTypesCount, Seq("study_id"), "left_outer")
      .join(combinedDataCategories, Seq("study_id"), "left_outer")
      .join(participantCount, Seq("study_id"), "left_outer")
      .join(fileCount, Seq("study_id"), "left_outer")
      .join(familyCount, Seq("study_id"), "left_outer")
      .expStrategiesCountPerStudy(data(normalized_task.id), data(normalized_drs_document_reference.id))
      .withColumn("family_data", col("family_count").gt(0))
      .withColumn("access_limitations", sparkTransform(filter(col("access_limitations"), col => col("display").isNotNull),
        col => concat_ws(" ", col("display"), concat(lit("("), col("code"), lit(")")))
      ))
      .withColumn("access_requirements", sparkTransform(filter(col("access_requirements"), col => col("display").isNotNull),
        col => concat_ws(" ", col("display"), concat(lit("("), col("code"), lit(")")))
      ))
      .withColumn("data_access_codes", struct(col("access_requirements"), col("access_limitations")))
      .withColumnRenamed("title", "name")
      .drop("fhir_id", "access_requirements", "access_limitations", "data_sets")

    Map(mainDestination.id -> transformedStudyDf)
  }
}
