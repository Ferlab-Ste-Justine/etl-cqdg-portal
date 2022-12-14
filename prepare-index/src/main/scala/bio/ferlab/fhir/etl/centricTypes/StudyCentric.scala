package bio.ferlab.fhir.etl.centricTypes

import bio.ferlab.datalake.commons.config.{Configuration, DatasetConf}
import bio.ferlab.datalake.spark3.etl.v2.ETL
import bio.ferlab.datalake.spark3.implicits.DatasetConfImplicits.DatasetConfOperations
import bio.ferlab.fhir.etl.common.OntologyUtils.displayTerm
import org.apache.spark.sql.functions._
import org.apache.spark.sql.{DataFrame, SparkSession}

import java.time.LocalDateTime

class StudyCentric(releaseId: String, studyIds: List[String])(implicit configuration: Configuration) extends ETL {

  override val mainDestination: DatasetConf = conf.getDataset("es_index_study_centric")
  val normalized_researchstudy: DatasetConf = conf.getDataset("normalized_research_study")
  val normalized_drs_document_reference: DatasetConf = conf.getDataset("normalized_document_reference")
  val normalized_patient: DatasetConf = conf.getDataset("normalized_patient")
  val normalized_group: DatasetConf = conf.getDataset("normalized_group")
  val normalized_diagnosis: DatasetConf = conf.getDataset("normalized_diagnosis")
  val normalized_task: DatasetConf = conf.getDataset("normalized_task")
  val normalized_phenotype: DatasetConf = conf.getDataset("normalized_phenotype")
  val hpo_terms: DatasetConf = conf.getDataset("hpo_terms")
  val mondo_terms: DatasetConf = conf.getDataset("mondo_terms")
  val icd_terms: DatasetConf = conf.getDataset("icd_terms")
  val duo_terms: DatasetConf = conf.getDataset("duo_terms")

  override def extract(lastRunDateTime: LocalDateTime = minDateTime,
                       currentRunDateTime: LocalDateTime = LocalDateTime.now())(implicit spark: SparkSession): Map[String, DataFrame] = {
    (Seq(
      normalized_researchstudy, normalized_drs_document_reference, normalized_patient, normalized_group, normalized_diagnosis, normalized_task, normalized_phenotype)
      .map(ds => ds.id -> ds.read.where(col("release_id") === releaseId)
        .where(col("study_id").isin(studyIds: _*))
      ) ++ Seq(
      hpo_terms.id -> hpo_terms.read,
      mondo_terms.id -> mondo_terms.read,
      icd_terms.id -> icd_terms.read,
      duo_terms.id -> duo_terms.read,
    )).toMap

  }

  override def transform(data: Map[String, DataFrame],
                         lastRunDateTime: LocalDateTime = minDateTime,
                         currentRunDateTime: LocalDateTime = LocalDateTime.now())(implicit spark: SparkSession): Map[String, DataFrame] = {

    val studyDF = data(normalized_researchstudy.id)

    val duoTerms = data(duo_terms.id).withColumn("duo_term", displayTerm(col("id"), col("name")))

    val accessLimitationsMap =
      studyDF
        .withColumn("access_limitations_exp", explode(col("access_limitations")))
        .join(duoTerms, col("access_limitations_exp") === col("id"), "left_outer")
        .groupBy("study_id")
        .agg(collect_list(col("duo_term")) as "access_limitations")

    val accessRequirementsMap =
      studyDF
        .withColumn("access_requirements_exp", explode(col("access_requirements")))
        .join(duoTerms, col("access_requirements_exp") === col("id"), "left_outer")
        .groupBy("study_id")
        .agg(collect_list(col("duo_term")) as "access_requirements")

    val filesExplodedDF = data(normalized_drs_document_reference.id)
      .withColumn("files_exp", explode(col("files")))
      .drop("study_id")

    val participantsWithFilesDF = data(normalized_patient.id)
      .withColumnRenamed("fhir_id", "participant_id")
      .join(filesExplodedDF, Seq("participant_id"), "left_outer")

    val dataTypesCount = participantsWithFilesDF
      .na.drop(Seq("data_type"))
      .groupBy("study_id", "data_type")
      .agg(size(collect_set(col("participant_id"))) as "participant_count")
      .groupBy("study_id")
      .agg(collect_list(struct(col("data_type"), col("participant_count"))) as "data_types")

    val dataCategoryCount = participantsWithFilesDF
      .na.drop(Seq("data_category"))
      .groupBy("study_id", "data_category")
      .agg(size(collect_set(col("participant_id"))) as "participant_count")
      .groupBy("study_id")
      .agg(collect_list(struct(col("data_category"), col("participant_count"))) as "data_categories")

    val participantCount =
      data(normalized_patient.id)
        .groupBy("study_id")
        .agg(size(collect_set("fhir_id")) as "participant_count")


    val fileCount =
      data(normalized_drs_document_reference.id)
        .withColumn("files_exploded", explode(col("files")))
        .groupBy("study_id")
        .agg(
          size(collect_set(
            struct(
              col("files_exploded")("file_name"),
              col("files_exploded")("file_format"),
              col("files_exploded")("ferload_url"))
          )) as "file_count",
          collect_set(col("data_category")) as "data_category"
        )

    val familyCount = data(normalized_group.id)
      .where(size(col("family_members")).gt(1))
      .groupBy("study_id")
      .agg(size(collect_set(col("internal_family_id"))) as "family_count")

    val experimentalStrategyGrouped = data(normalized_task.id)
      .withColumn("experimental_strategy_exp", explode(col("experimental_strategy")))
      .groupBy("study_id")
      .agg(collect_set(col("experimental_strategy_exp")) as "experimental_strategy")

    val phenotypeGrouped = data(normalized_phenotype.id)
      .withColumn("phenotype", col("phenotype_HPO_code")("code"))
      .join(data(hpo_terms.id), col("phenotype") === col("id"), "left_outer")
      .withColumn("hpo_term", displayTerm(col("id"), col("name")))
      .groupBy("study_id")
      .agg(collect_set(col("hpo_term")) as "hpo_terms")

    // the id is of the from id|chapter. See why it was done like this, and if still required
    val icdSplitId = data(icd_terms.id)
      .withColumn("splitId", split(col("id"), "\\|")(0))
    val diagnosisGrouped = data(normalized_diagnosis.id)
      .join(data(mondo_terms.id), col("diagnosis_mondo_code") === col("id"), "left_outer")
      .withColumn("mondo_term", displayTerm(col("id"), col("name")))
      .drop("id", "name")
      .join(icdSplitId, col("diagnosis_ICD_code") === col("splitId"), "left_outer")
      .withColumn("icd_term", displayTerm(col("splitId"), col("name")))
      .groupBy("study_id")
      .agg(collect_set(col("mondo_term")) as "mondo_terms", collect_set(col("icd_term")) as "icd_terms")

    val transformedStudyDf = studyDF
      .join(dataTypesCount, Seq("study_id"), "left_outer")
      .join(dataCategoryCount, Seq("study_id"), "left_outer")
      .join(participantCount, Seq("study_id"), "left_outer")
      .join(fileCount, Seq("study_id"), "left_outer")
      .join(familyCount, Seq("study_id"), "left_outer")
      .join(experimentalStrategyGrouped, Seq("study_id"), "left_outer")
      .join(phenotypeGrouped, Seq("study_id"), "left_outer")
      .join(diagnosisGrouped, Seq("study_id"), "left_outer")
      .withColumn("family_data", col("family_count").gt(0))
      .drop("access_limitations", "access_requirements")
      .join(accessLimitationsMap, Seq("study_id"), "left_outer")
      .join(accessRequirementsMap, Seq("study_id"), "left_outer")
      .withColumn("data_access_codes", struct(col("access_requirements"), col("access_limitations")))
      .withColumnRenamed("title", "name")
      .drop("fhir_id", "access_requirements", "access_limitations")

    Map(mainDestination.id -> transformedStudyDf)
  }
}
