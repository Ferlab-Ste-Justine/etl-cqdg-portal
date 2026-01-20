package bio.ferlab.etl.normalized

import bio.ferlab.datalake.commons.config.{DatasetConf, RuntimeETLContext}
import bio.ferlab.datalake.spark3.genomics.normalized.BaseConsequences
import bio.ferlab.datalake.spark3.implicits.GenomicImplicits.columns._
import bio.ferlab.datalake.spark3.implicits.GenomicImplicits.vcf
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.functions.{array_contains, col, lit}

import java.time.LocalDateTime

case class Consequences(
    rc: RuntimeETLContext,
    studyCode: String,
    owner: String,
    dataset: String,
    batch: String,
    referenceGenomePath: Option[String]
) extends BaseConsequences(rc: RuntimeETLContext, annotationsColumn = csq, groupByLocus = true) {
  private val raw_variant_calling: DatasetConf = conf.getDataset("raw_vcf")
  override val mainDestination: DatasetConf = conf.getDataset("normalized_consequences")

  override def extract(
      lastRunDateTime: LocalDateTime = minValue,
      currentRunDateTime: LocalDateTime = LocalDateTime.now()
  ): Map[String, DataFrame] = {
    Map(
      raw_vcf -> vcf(
        raw_variant_calling.location
          .replace("{{STUDY_CODE}}", s"$studyCode")
          .replace("{{DATASET}}", s"$dataset")
          .replace("{{BATCH}}", s"$batch")
          .replace("{{OWNER}}", s"$owner"),
        referenceGenomePath = None
      )
        .where(col("contigName").isin(validContigNames: _*))
    )
  }

  override def transformSingle(
      data: Map[String, DataFrame],
      lastRunDateTime: LocalDateTime,
      currentRunDateTime: LocalDateTime
  ): DataFrame = {
    // Remove low quality variants
    val filteredVcf = data(raw_vcf).filter(array_contains(col("INFO_FILTERS"), "PASS"))

    super
      .transformSingle(data + (raw_vcf -> filteredVcf), lastRunDateTime, currentRunDateTime)
      .withColumn("study_id", lit(studyCode))
  }
}
