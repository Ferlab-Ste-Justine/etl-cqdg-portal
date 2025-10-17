package bio.ferlab.fhir.etl.centricTypes

import bio.ferlab.datalake.commons.config.{Configuration, DatasetConf}
import bio.ferlab.datalake.spark3.etl.v2.ETL
import bio.ferlab.datalake.spark3.implicits.DatasetConfImplicits._
import bio.ferlab.fhir.etl.common.Utils._
import org.apache.spark.sql.functions.{col, struct}
import org.apache.spark.sql.{DataFrame, SparkSession}

import java.time.LocalDateTime

class SimpleParticipant(studyIds: List[String])(implicit configuration: Configuration) extends ETL {

  override val mainDestination: DatasetConf = conf.getDataset("simple_participant")
  val normalized_patient: DatasetConf = conf.getDataset("normalized_patient")
  val normalized_phenotype: DatasetConf = conf.getDataset("normalized_phenotype")
  val normalized_disease: DatasetConf = conf.getDataset("normalized_diagnosis")
  val normalized_disease_status: DatasetConf = conf.getDataset("normalized_disease_status")
  val normalized_cause_of_death: DatasetConf = conf.getDataset("normalized_cause_of_death")
  val normalized_group: DatasetConf = conf.getDataset("normalized_group")
  val normalized_family_relationship: DatasetConf = conf.getDataset("normalized_family_relationship")
  val normalized_researchstudy: DatasetConf = conf.getDataset("normalized_research_study")


  val hpo_terms: DatasetConf = conf.getDataset("hpo_terms")
  val mondo_terms: DatasetConf = conf.getDataset("mondo_terms")
  val icd_terms: DatasetConf = conf.getDataset("icd_terms")

  override def extract(lastRunDateTime: LocalDateTime = minDateTime,
                       currentRunDateTime: LocalDateTime = LocalDateTime.now())(implicit spark: SparkSession): Map[String, DataFrame] = {
    (Seq(
      normalized_patient, normalized_phenotype, normalized_disease, normalized_disease_status, normalized_cause_of_death,
      normalized_group, normalized_family_relationship, normalized_researchstudy)
      .map(ds => ds.id -> ds.read.where(col("study_id").isin(studyIds: _*))
      ) ++ Seq(
      hpo_terms.id -> hpo_terms.read,
      mondo_terms.id -> mondo_terms.read,
      icd_terms.id -> icd_terms.read,
    )).toMap

  }

  override def transform(data: Map[String, DataFrame],
                         lastRunDateTime: LocalDateTime = minDateTime,
                         currentRunDateTime: LocalDateTime = LocalDateTime.now())(implicit spark: SparkSession): Map[String, DataFrame] = {

    val patientDF = data(normalized_patient.id)

    val shortStudyCode = data(normalized_researchstudy.id)
      .select("fhir_id", "study_code")
      .withColumn("study", struct(col("study_code")))
      .withColumnRenamed("fhir_id", "study_id")

    val transformedParticipant =
      patientDF
        .addCauseOfDeath(data(normalized_cause_of_death.id))
        .addDiseaseStatus(data(normalized_disease_status.id))
        .addDiagnosisPhenotypes(
          data(normalized_phenotype.id),
          data(normalized_disease.id)
        )(data(hpo_terms.id), data(mondo_terms.id), data(icd_terms.id))
        .addFamily(data(normalized_group.id), data(normalized_family_relationship.id))
        .join(shortStudyCode, Seq("study_id"), "left_outer")
        .withColumn("participant_2_id", col("participant_id")) //Duplicate for UI purpose

    patientDF.show(false)
    println("-----------1----")
    patientDF
      .addCauseOfDeath(data(normalized_cause_of_death.id))
      .addDiseaseStatus(data(normalized_disease_status.id))
      .show(false)
    println("------------2---")
    patientDF
      .addCauseOfDeath(data(normalized_cause_of_death.id))
      .addDiseaseStatus(data(normalized_disease_status.id))
      .addDiagnosisPhenotypes(
        data(normalized_phenotype.id),
        data(normalized_disease.id)
      )(data(hpo_terms.id), data(mondo_terms.id), data(icd_terms.id))
      .show(false)
    println("------------3---")
    patientDF
      .addCauseOfDeath(data(normalized_cause_of_death.id))
      .addDiseaseStatus(data(normalized_disease_status.id))
      .addDiagnosisPhenotypes(
        data(normalized_phenotype.id),
        data(normalized_disease.id)
      )(data(hpo_terms.id), data(mondo_terms.id), data(icd_terms.id))
      .addFamily(data(normalized_group.id), data(normalized_family_relationship.id))
      .show(false)
    println("------------4---")
    patientDF
      .addCauseOfDeath(data(normalized_cause_of_death.id))
      .addDiseaseStatus(data(normalized_disease_status.id))
      .addDiagnosisPhenotypes(
        data(normalized_phenotype.id),
        data(normalized_disease.id)
      )(data(hpo_terms.id), data(mondo_terms.id), data(icd_terms.id))
      .addFamily(data(normalized_group.id), data(normalized_family_relationship.id))
      .join(shortStudyCode, Seq("study_id"), "left_outer")
    println("--------------F-")
    transformedParticipant.show(false)

    Map(mainDestination.id -> transformedParticipant)
  }
}
