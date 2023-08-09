package bio.ferlab.etl.normalized

import bio.ferlab.datalake.commons.config.{DatasetConf, RuntimeETLContext}
import bio.ferlab.datalake.spark3.genomics.normalized.BaseConsequences
import bio.ferlab.datalake.spark3.implicits.GenomicImplicits.columns._
import bio.ferlab.datalake.spark3.implicits.GenomicImplicits.vcf
import bio.ferlab.etl.mainutils.Study
import mainargs.{ParserForMethods, main}
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.functions.col

import java.time.LocalDateTime

case class Consequences(rc: RuntimeETLContext, studyId: String) extends BaseConsequences(rc: RuntimeETLContext, annotationsColumn = csq, groupByLocus = true) {
  private val raw_variant_calling: DatasetConf = conf.getDataset("raw_vcf")
  override val mainDestination: DatasetConf = conf.getDataset("normalized_consequences")

  override def extract(lastRunDateTime: LocalDateTime = minDateTime,
                       currentRunDateTime: LocalDateTime = LocalDateTime.now()): Map[String, DataFrame] = {
    Map(
      raw_vcf -> vcf(raw_variant_calling.location.replace("{{STUDY_ID}}", s"${studyId}_test"), referenceGenomePath = None) //TODO remove "test" when server is available
        .where(col("contigName").isin(validContigNames: _*))
    )
  }
}

object Consequences {
  @main
  def run(rc: RuntimeETLContext, study: Study): Unit = {
    Consequences(rc, study.id).run()
  }

  def main(args: Array[String]): Unit = ParserForMethods(this).runOrThrow(args)
}
