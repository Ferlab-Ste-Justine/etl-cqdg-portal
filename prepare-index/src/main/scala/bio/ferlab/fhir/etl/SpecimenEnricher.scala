package bio.ferlab.fhir.etl

import bio.ferlab.datalake.commons.config.{Configuration, DatasetConf}
import bio.ferlab.datalake.spark3.etl.ETLSingleDestination
import bio.ferlab.datalake.spark3.implicits.DatasetConfImplicits.DatasetConfOperations
import org.apache.spark.sql.functions._
import org.apache.spark.sql.{DataFrame, SparkSession, functions}
import bio.ferlab.fhir.etl.common.Utils._
import org.apache.spark.sql.expressions.UserDefinedFunction

import java.time.LocalDateTime

/**
 * This step enrich specimen in order to join with occurrences and variant tables.
 * @param studyIds
 * @param configuration
 */
class SpecimenEnricher(studyIds: List[String])(implicit configuration: Configuration) extends ETLSingleDestination {
  override val mainDestination: DatasetConf = conf.getDataset("enriched_specimen")
  private val patient: DatasetConf = conf.getDataset("normalized_patient")
  private val group: DatasetConf = conf.getDataset("normalized_group")
  private val family_relationship: DatasetConf = conf.getDataset("normalized_family_relationship")
  private val specimen: DatasetConf = conf.getDataset("normalized_biospecimen")
  private val sample_registration: DatasetConf = conf.getDataset("normalized_sample_registration")
  private val study: DatasetConf = conf.getDataset("normalized_research_study")
  private val disease: DatasetConf = conf.getDataset("normalized_diagnosis")
  private val disease_status: DatasetConf = conf.getDataset("normalized_disease_status")

  val extractParent: String => UserDefinedFunction = (parent: String) =>
    udf(
      (arr: Seq[(String, String)])
      => arr.find(e => e._2 == parent).map(e=> e._1))

  override def extract(lastRunDateTime: LocalDateTime, currentRunDateTime: LocalDateTime)(implicit spark: SparkSession): Map[String, DataFrame] = {
    Seq(patient, specimen, group, family_relationship, sample_registration, study, disease, disease_status)
      .map(ds => ds.id -> ds.read
        .where(col("study_id").isin(studyIds: _*))
      ).toMap
  }

  override def transformSingle(data: Map[String, DataFrame], lastRunDateTime: LocalDateTime, currentRunDateTime: LocalDateTime)(implicit spark: SparkSession): DataFrame = {
    val participantWithFam = data(patient.id)
      .withColumn("participant_id", col("fhir_id"))
      .join(data(disease_status.id).drop("fhir_id", "study_id", "release_id"), col("participant_id") === col("subject"), "left_outer")
      .withColumn("is_affected",
        when(col("disease_status").isNotNull and col("disease_status") === "yes", true).otherwise(false)
      )
      .drop("disease_status", "subject")
      .addFamily(data(group.id), data(family_relationship.id))

    val studyShort = data(study.id).select("fhir_id", "study_code").withColumnRenamed("fhir_id", "study_id")

    val parents = data(family_relationship.id)
      .groupBy("study_id", "internal_family_relationship_id")
      .agg(collect_list(struct( "submitter_participant_id", "relationship_to_proband")) as "groupedFam")
      .withColumn("father_id",  extractParent("father")(col("groupedFam")))
      .withColumn("mother_id",  extractParent("mother")(col("groupedFam")))
      .withColumn("expFamily", explode(col("groupedFam")))
      .withColumn("subject", col("expFamily.submitter_participant_id"))
      .withColumnRenamed("internal_family_relationship_id", "family_id")
      .select("subject","mother_id", "father_id", "family_id")

    val patientDf = participantWithFam
      .withColumnRenamed("participant_id", "subject")
      .withColumnRenamed("fhir_id", "participant_fhir_id")
      .select("subject", "gender", "vital_status", "ethnicity", "is_a_proband", "is_affected", "participant_fhir_id", "submitter_participant_id")

    data(specimen.id)
      .join(patientDf, Seq("subject"))
      .addSamplesToBiospecimen(data(sample_registration.id))
      .withColumnRenamed("fhir_id", "biospecimen_id")
      .withColumnRenamed("sample_id", "fhir_sample_id")
      .withColumnRenamed("submitter_sample_id", "sample_id")
      .join(parents, Seq("subject"), "left_outer")
      .withColumnRenamed("subject", "participant_id")
      .join(studyShort, Seq("study_id"))
  }
}
