package bio.ferlab.fhir.etl.centricTypes

import bio.ferlab.datalake.commons.config.{Configuration, DatasetConf}
import bio.ferlab.datalake.spark3.etl.v2.ETL
import bio.ferlab.datalake.spark3.implicits.DatasetConfImplicits.DatasetConfOperations
import bio.ferlab.fhir.etl.common.Utils._
import bio.ferlab.datalake.spark3.loader.GenericLoader.read
import org.apache.spark.sql.functions.{array, coalesce, col, collect_list, collect_set, count, explode, lit, shuffle, struct}
import org.apache.spark.sql.{DataFrame, SparkSession}

import java.time.LocalDateTime

class StudyCentric(releaseId: String, studyIds: List[String])(implicit configuration: Configuration) extends ETL {

  override val mainDestination: DatasetConf = conf.getDataset("es_index_study_centric")
  val normalized_researchstudy: DatasetConf = conf.getDataset("normalized_research_study")
  val normalized_drs_document_reference: DatasetConf = conf.getDataset("normalized_document_reference")
  val normalized_patient: DatasetConf = conf.getDataset("normalized_patient")
  val normalized_family_relationship: DatasetConf = conf.getDataset("normalized_family_relationship")
  val normalized_group: DatasetConf = conf.getDataset("normalized_group")
  val normalized_specimen: DatasetConf = conf.getDataset("normalized_biospecimen")
  val normalized_cause_of_death: DatasetConf = conf.getDataset("normalized_cause_of_death")
  val normalized_diagnosis: DatasetConf = conf.getDataset("normalized_diagnosis")

  override def extract(lastRunDateTime: LocalDateTime = minDateTime,
                       currentRunDateTime: LocalDateTime = LocalDateTime.now())(implicit spark: SparkSession): Map[String, DataFrame] = {
    Seq(normalized_researchstudy, normalized_drs_document_reference, normalized_patient, normalized_group, normalized_specimen, normalized_family_relationship, normalized_cause_of_death, normalized_diagnosis)
      .map(ds => ds.id -> ds.read.where(col("release_id") === releaseId)
        .where(col("study_id").isin(studyIds: _*))
      ).toMap

  }

  override def transform(data: Map[String, DataFrame],
                         lastRunDateTime: LocalDateTime = minDateTime,
                         currentRunDateTime: LocalDateTime = LocalDateTime.now())(implicit spark: SparkSession): Map[String, DataFrame] = {
    val studyDF = data(normalized_researchstudy.id)

    val familyRelationshipDf = data(normalized_family_relationship.id)
      .addGroup(data(normalized_group.id))

    val patientDf = data(normalized_patient.id)
      .addFamilyRelationshipToParticipant(familyRelationshipDf)
      .join(data(normalized_cause_of_death.id).drop("study_id", "release_id", "fhir_id"), Seq("submitter_participant_ids"), "left_outer")

    val patientsPerStudy =
      patientDf
        .groupBy("study_id", "release_id")
        .agg(
          collect_list(struct(patientDf.columns.filter(c => !Seq("fhir_id", "study_id").contains(c)).map(col): _*)) as "participants"
        )

    patientsPerStudy.show(false)
    patientsPerStudy.printSchema()

//    data(normalized_researchstudy.id).show(false)
//    data(normalized_patient.id).show(false)
//    data(normalized_cause_of_death.id).show(false)
//    data(normalized_family_relationship.id).show(false)
//    data(normalized_group.id).show(false)
    patientDf.show(false)

    val transformedStudyDf = studyDF
      .withColumn("data_access_codes", struct(col("access_limitations") as "access_limitations", col("access_requirements") as "access_requirements"))
      .withColumnRenamed("fhir_id", "internal_study_id")
//      .join(familyRelationshipDf, col("internal_study_id") === col("submitter_participant_id"), "left_outer")
//      .join(countPatientDf, Seq("study_id"), "left_outer")
//      .withColumn("participant_count", coalesce(col("participant_count"), lit(0)))
//      .join(countFileDf, Seq("study_id"), "left_outer")
//      .join(countBiospecimenDf, Seq("study_id"), "left_outer")
//      .withColumn("file_count", coalesce(col("file_count"), lit(0)))
//      .join(countFamilyDf, Seq("study_id"), "left_outer")
//      .withColumn("family_count", coalesce(col("family_count"), lit(0)))
//      .withColumn("family_data", col("family_count").gt(0))

    Map(mainDestination.id -> transformedStudyDf)
  }
}
