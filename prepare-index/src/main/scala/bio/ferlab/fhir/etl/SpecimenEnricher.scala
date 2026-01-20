package bio.ferlab.fhir.etl

import bio.ferlab.datalake.commons.config.{DatasetConf, RuntimeETLContext}
import bio.ferlab.datalake.spark3.etl.v4.SingleETL
import bio.ferlab.datalake.spark3.implicits.DatasetConfImplicits.DatasetConfOperations
import bio.ferlab.fhir.etl.common.Utils._
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.functions._

import java.time.LocalDateTime

/** This step enrich specimen in order to join with occurrences and variant tables.
  *
  * @param studyIds
  * @param configuration
  */
case class SpecimenEnricher(rc: RuntimeETLContext, studyIds: Seq[String]) extends SingleETL(rc) {
  override val mainDestination: DatasetConf = conf.getDataset("enriched_specimen")
  private val patient: DatasetConf = conf.getDataset("normalized_patient")
  private val group: DatasetConf = conf.getDataset("normalized_group")
  private val family_relationship: DatasetConf = conf.getDataset("normalized_family_relationship")
  private val specimen: DatasetConf = conf.getDataset("normalized_biospecimen")
  private val sample_registration: DatasetConf = conf.getDataset("normalized_sample_registration")
  private val study: DatasetConf = conf.getDataset("normalized_research_study")
  private val disease: DatasetConf = conf.getDataset("normalized_diagnosis")
  private val disease_status: DatasetConf = conf.getDataset("normalized_disease_status")
  private val ncit_terms: DatasetConf = conf.getDataset("ncit_terms")

  override def extract(lastRunDateTime: LocalDateTime, currentRunDateTime: LocalDateTime): Map[String, DataFrame] = {
    Seq(patient, specimen, group, family_relationship, sample_registration, study, disease, disease_status)
      .map(ds =>
        ds.id -> ds.read
          .where(col("study_id").isin(studyIds: _*))
      )
      .toMap
  }

  override def transformSingle(
      data: Map[String, DataFrame],
      lastRunDateTime: LocalDateTime,
      currentRunDateTime: LocalDateTime
  ): DataFrame = {
    val participantWithFam = data(patient.id)
      .withColumn("participant_id", col("fhir_id"))
      .join(data(disease_status.id).drop("fhir_id", "study_id"), col("participant_id") === col("subject"), "left_outer")
      .withColumn(
        "is_affected",
        when(col("disease_status").isNotNull and col("disease_status") === "yes", true).otherwise(false)
      )
      .drop("disease_status", "subject")
      .addFamily(data(group.id), data(family_relationship.id))

    val studyShort = data(study.id).select("fhir_id", "study_code").withColumnRenamed("fhir_id", "study_id")

    val patientDf = participantWithFam
      .withColumn(
        "parent_ids",
        aggregate(
          col("family_relationships"),
          struct(lit(null).cast("string") as "father_id", lit(null).cast("string") as "mother_id"),
          (acc, relation) =>
            when(
              relation("relationship_to_proband") === "Father",
              struct(relation("participant_id") as "father_id", acc("mother_id") as "mother_id")
            )
              .when(
                relation("relationship_to_proband") === "Mother",
                struct(acc("father_id") as "father_id", relation("participant_id") as "mother_id")
              )
              .otherwise(acc)
        )
      )
      .withColumn("father_id", when(col("is_a_proband"), col("parent_ids.father_id")).otherwise(lit(null)))
      .withColumn("mother_id", when(col("is_a_proband"), col("parent_ids.mother_id")).otherwise(lit(null)))
      .drop("parent_ids")
      .withColumnRenamed("participant_id", "subject")
      .withColumnRenamed("fhir_id", "participant_fhir_id")
      .select(
        "subject",
        "sex",
        "vital_status",
        "ethnicity",
        "is_a_proband",
        "is_affected",
        "participant_fhir_id",
        "submitter_participant_id",
        "family_id",
        "father_id",
        "mother_id"
      )

    data(specimen.id)
      .joinNcitTerms(data(ncit_terms.id), "biospecimen_tissue_source")
      .join(patientDf, Seq("subject"))
      .addSamplesToBiospecimen(data(sample_registration.id).joinNcitTerms(data(ncit_terms.id), "sample_type"))
      .withColumnRenamed("fhir_id", "biospecimen_id")
      .withColumnRenamed("sample_id", "fhir_sample_id")
      .withColumnRenamed("submitter_sample_id", "sample_id")
      .withColumnRenamed("subject", "participant_id")
      .join(studyShort, Seq("study_id"))
  }
}
