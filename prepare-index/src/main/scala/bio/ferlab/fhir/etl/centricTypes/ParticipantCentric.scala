package bio.ferlab.fhir.etl.centricTypes

import bio.ferlab.datalake.commons.config.{Configuration, DatasetConf}
import bio.ferlab.datalake.spark3.etl.v2.ETL
import bio.ferlab.datalake.spark3.implicits.DatasetConfImplicits.DatasetConfOperations
import bio.ferlab.fhir.etl.common.Utils._
import org.apache.spark.sql.functions.col
import org.apache.spark.sql.{DataFrame, SparkSession}

import java.time.LocalDateTime

class ParticipantCentric(releaseId: String, studyIds: List[String])(implicit configuration: Configuration) extends ETL {

  override val mainDestination: DatasetConf = conf.getDataset("es_index_participant_centric")
  val simple_participant: DatasetConf = conf.getDataset("simple_participant")
  val normalized_drs_document_reference: DatasetConf = conf.getDataset("normalized_document_reference")
  val normalized_sequencing_experiment: DatasetConf = conf.getDataset("normalized_task")
  val normalized_biospecimen: DatasetConf = conf.getDataset("normalized_biospecimen")
  val normalized_sample_registration: DatasetConf = conf.getDataset("normalized_sample_registration")
  val es_index_study_centric: DatasetConf = conf.getDataset("es_index_study_centric")

  override def extract(lastRunDateTime: LocalDateTime = minDateTime,
                       currentRunDateTime: LocalDateTime = LocalDateTime.now())(implicit spark: SparkSession): Map[String, DataFrame] = {
    Seq(simple_participant, normalized_drs_document_reference, normalized_biospecimen, normalized_sequencing_experiment, normalized_sample_registration, es_index_study_centric)
      .map(ds => ds.id -> ds.read.where(col("release_id") === releaseId)
                            .where(col("study_id").isin(studyIds: _*))
      ).toMap
  }

  override def transform(data: Map[String, DataFrame],
                         lastRunDateTime: LocalDateTime = minDateTime,
                         currentRunDateTime: LocalDateTime = LocalDateTime.now())(implicit spark: SparkSession): Map[String, DataFrame] = {
    val patientDF = data(simple_participant.id)

    val transformedParticipant =
      patientDF
        .addStudy(data(es_index_study_centric.id))
        .addFilesWithBiospecimen (
          data(normalized_drs_document_reference.id),
          data(normalized_biospecimen.id),
          data(normalized_sequencing_experiment.id),
          data(normalized_sample_registration.id),
        )
        .withColumn("study_code", col("study.study_code"))

    Map(mainDestination.id -> transformedParticipant)
  }
}
