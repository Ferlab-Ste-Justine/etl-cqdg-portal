package bio.ferlab.fhir.etl.centricTypes

import bio.ferlab.datalake.commons.config.{Configuration, DatasetConf}
import bio.ferlab.datalake.spark3.etl.v2.ETL
import bio.ferlab.datalake.spark3.implicits.DatasetConfImplicits.DatasetConfOperations
import bio.ferlab.fhir.etl.common.Utils._
import org.apache.spark.sql.functions.{array_distinct, col, concat, explode, flatten, lit, struct}
import org.apache.spark.sql.{DataFrame, SparkSession, functions}

import java.time.LocalDateTime

class FileCentric(studyIds: List[String])(implicit configuration: Configuration) extends ETL {

  override val mainDestination: DatasetConf = conf.getDataset("es_index_file_centric")
  val normalized_drs_document_reference: DatasetConf = conf.getDataset("normalized_document_reference")
  val normalized_biospecimen: DatasetConf = conf.getDataset("normalized_biospecimen")
  val normalized_sequencing_experiment: DatasetConf = conf.getDataset("normalized_task")
  val normalized_sample_registration: DatasetConf = conf.getDataset("normalized_sample_registration")
  val simple_participant: DatasetConf = conf.getDataset("simple_participant")
  val es_index_study_centric: DatasetConf = conf.getDataset("es_index_study_centric")
  val ncit_terms: DatasetConf = conf.getDataset("ncit_terms")

  override def extract(lastRunDateTime: LocalDateTime = minDateTime,
                       currentRunDateTime: LocalDateTime = LocalDateTime.now())(implicit spark: SparkSession): Map[String, DataFrame] = {
    (Seq(normalized_biospecimen, simple_participant, es_index_study_centric,
      normalized_sequencing_experiment, normalized_sample_registration)
      .map(ds => ds.id -> ds.read.where(col("study_id").isin(studyIds: _*))) ++
      Seq(
        ncit_terms.id -> ncit_terms.read,
        normalized_drs_document_reference.id -> normalized_drs_document_reference.read
          .where(col("study_id").isin(studyIds: _*))
          .where(col("security") =!= "R")
      )).toMap
  }

  override def transform(data: Map[String, DataFrame],
                         lastRunDateTime: LocalDateTime = minDateTime,
                         currentRunDateTime: LocalDateTime = LocalDateTime.now())(implicit spark: SparkSession): Map[String, DataFrame] = {
    val fileDF = data(normalized_drs_document_reference.id)

    val transformedFile =
      fileDF
        .addParticipantWithBiospecimen(
          data(simple_participant.id),
          data(normalized_biospecimen.id).joinNcitTerms(data(ncit_terms.id), "biospecimen_tissue_source"),
          data(normalized_sample_registration.id).joinNcitTerms(data(ncit_terms.id), "sample_type")
        )
        .addStudy(data(es_index_study_centric.id))
        .addSequencingExperiment(data(normalized_sequencing_experiment.id))
        .addAssociatedDocumentRef()
        .withColumnRenamed("fhir_id", "file_id")
        .withColumn("study_code", col("study.study_code"))
        .withColumn("biospecimens", array_distinct(flatten(col("participants.biospecimens"))))
        .withColumn("file_2_id", col("file_id")) //copy column/ front-end requirements

    Map(mainDestination.id -> transformedFile)
  }
}
