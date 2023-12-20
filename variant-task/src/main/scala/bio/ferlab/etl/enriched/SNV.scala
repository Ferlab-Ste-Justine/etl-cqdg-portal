package bio.ferlab.etl.enriched

import bio.ferlab.datalake.commons.config.{DatasetConf, RuntimeETLContext}
import bio.ferlab.datalake.spark3.etl.v4.SimpleSingleETL
import bio.ferlab.datalake.spark3.implicits.DatasetConfImplicits.DatasetConfOperations
import bio.ferlab.etl.Constants.columns.TRANSMISSION_MODE
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.functions.col
import bio.ferlab.datalake.spark3.implicits.GenomicImplicits._

import java.time.LocalDateTime

case class SNV(rc: RuntimeETLContext, studyId: String, dataset: String, batch: String) extends SimpleSingleETL(rc) {

  override val mainDestination: DatasetConf = conf.getDataset("enriched_snv")
  private val normalized_snv: DatasetConf = conf.getDataset("normalized_snv")

  override def extract(lastRunValue: LocalDateTime, currentRunValue: LocalDateTime): Map[String, DataFrame] = Map(
    normalized_snv.id -> normalized_snv.read.where(s"study_id = '$studyId' and dataset='$dataset' and batch='$batch' ")
  )

  override def transformSingle(data: Map[String, DataFrame], lastRunValue: LocalDateTime, currentRunValue: LocalDateTime): DataFrame = {
    data(normalized_snv.id)
      .withRelativesGenotype(Seq("gq", "dp", "info_qd", "filter", "ad_ref", "ad_alt", "ad_total", "ad_ratio", "calls", "affected_status", "zygosity"),
        participantIdColumn = col("participant_id"),
        familyIdColumn = col("family_id")
      )
      .withParentalOrigin("parental_origin", col("calls"), col("father_calls"), col("mother_calls"))
      .withGenotypeTransmission(TRANSMISSION_MODE, `gender_name` = "gender")
  }

  override def replaceWhere: Option[String] = Some(s"study_id = '$studyId' and dataset='$dataset' and batch='$batch' ")
}
