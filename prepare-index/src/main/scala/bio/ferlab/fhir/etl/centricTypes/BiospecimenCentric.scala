package bio.ferlab.fhir.etl.centricTypes

import bio.ferlab.datalake.commons.config.{Configuration, DatasetConf}
import bio.ferlab.datalake.spark3.etl.v2.ETL
import bio.ferlab.datalake.spark3.implicits.DatasetConfImplicits.DatasetConfOperations
import bio.ferlab.fhir.etl.common.Utils._
import org.apache.spark.sql.functions.{col, struct}
import org.apache.spark.sql.{DataFrame, SparkSession}

import java.time.LocalDateTime

class BiospecimenCentric(releaseId: String, studyIds: List[String])(implicit configuration: Configuration) extends ETL {

  override val mainDestination: DatasetConf = conf.getDataset("es_index_biospecimen_centric")
  val normalized_biospecimen: DatasetConf = conf.getDataset("normalized_biospecimen")
  val normalized_drs_document_reference: DatasetConf = conf.getDataset("normalized_document_reference")
  val normalized_sequencing_experiment: DatasetConf = conf.getDataset("normalized_task")
  val normalized_sample_registration: DatasetConf = conf.getDataset("normalized_sample_registration")
  val simple_participant: DatasetConf = conf.getDataset("simple_participant")
  val es_index_study_centric: DatasetConf = conf.getDataset("es_index_study_centric")
  val es_index_file_centric: DatasetConf = conf.getDataset("es_index_file_centric")

  override def extract(lastRunDateTime: LocalDateTime = minDateTime,
                       currentRunDateTime: LocalDateTime = LocalDateTime.now())(implicit spark: SparkSession): Map[String, DataFrame] = {

    Seq(normalized_biospecimen, normalized_drs_document_reference, simple_participant, es_index_study_centric, normalized_sequencing_experiment, normalized_sample_registration, es_index_file_centric)
      .map(ds => ds.id -> ds.read.where(col("release_id") === releaseId)
        .where(col("study_id").isin(studyIds: _*))
      ).toMap
  }

  override def transform(data: Map[String, DataFrame],
                         lastRunDateTime: LocalDateTime = minDateTime,
                         currentRunDateTime: LocalDateTime = LocalDateTime.now())(implicit spark: SparkSession): Map[String, DataFrame] = {
    val biospecimenDF = data(normalized_biospecimen.id)

    val transformedBiospecimen =
      biospecimenDF
        .withColumn("biospecimen_tissue_source", col("biospecimen_tissue_source")("code"))
        .withColumn("age_biospecimen_collection", col("age_biospecimen_collection")("value"))
        .addStudy(data(es_index_study_centric.id))
        .addParticipant(data(simple_participant.id))
        .addFiles(data(normalized_drs_document_reference.id), data(normalized_sequencing_experiment.id))
        .addSamplesToBiospecimen(data(normalized_sample_registration.id))

    Map(mainDestination.id -> transformedBiospecimen)
  }
}
