package bio.ferlab.etl.normalize

import bio.ferlab.datalake.commons.config.{Configuration, DatasetConf}
import bio.ferlab.datalake.spark3.genomics.normalized.BaseConsequences
import bio.ferlab.datalake.spark3.implicits.GenomicImplicits.columns._
import bio.ferlab.datalake.spark3.implicits.GenomicImplicits.vcf
import org.apache.spark.sql.functions.col
import org.apache.spark.sql.{DataFrame, SparkSession}

import java.time.LocalDateTime

class Consequences(studyId: String)(implicit configuration: Configuration) extends BaseConsequences(annotationsColumn = csq, groupByLocus = true) {
  private val raw_variant_calling: DatasetConf = conf.getDataset("raw_vcf")
  override val mainDestination: DatasetConf = conf.getDataset("normalized_consequences")

  override def extract(lastRunDateTime: LocalDateTime = minDateTime,
                       currentRunDateTime: LocalDateTime = LocalDateTime.now())(implicit spark: SparkSession): Map[String, DataFrame] = {
    Map(
      raw_vcf -> vcf(raw_variant_calling.location.replace("{{STUDY_ID}}", s"${studyId}_test"), referenceGenomePath = None) //TODO remove "test" when server is available
        .where(col("contigName").isin(validContigNames: _*))
    )
  }

}
