package bio.ferlab.etl.enriched

import bio.ferlab.datalake.commons.config.RuntimeETLContext
import bio.ferlab.datalake.spark3.genomics.enriched.{Consequences, Variants}
import bio.ferlab.datalake.spark3.genomics.{FirstElement, FrequencySplit, SimpleAggregation, SimpleSplit}
import mainargs.{ParserForMethods, arg, main}
import org.apache.spark.sql.functions._

object RunEnrichGenomic {

  @main
  def snv(
      rc: RuntimeETLContext,
      @arg(name = "study-code", short = 's', doc = "Study Code") studyCode: String,
      @arg(name = "dataset", short = 'd', doc = "Dataset") dataset: String,
      @arg(name = "batch", short = 'b', doc = "Batch") batch: String
  ): Unit = SNV(rc, studyCode, dataset, batch).run()

  @main
  def variants(rc: RuntimeETLContext): Unit = runVariants(rc).run()

  @main
  def consequences(rc: RuntimeETLContext): Unit = Consequences(rc).run()

  @main
  def all(rc: RuntimeETLContext): Unit = {
    variants(rc)
    consequences(rc)
  }

  def runVariants(rc: RuntimeETLContext): Variants = Variants(
    rc = rc,
    snvDatasetId = "normalized_snv",
    extraAggregations = Seq(collect_set("source") as "sources"),
    splits = Seq(
      SimpleSplit(
        name = "studies",
        extraSplitBy = Some(col("study_id")),
        aggregations = Seq(
          FirstElement("study_code", col("study_code")),
          SimpleAggregation(name = "zygosity", c = col("zygosity"))
        )
      ),
      FrequencySplit(
        "study_frequencies_wgs",
        filter = Some(col("source") === "WGS"),
        extraSplitBy = Some(col("study_id")),
        byAffected = false,
        extraAggregations = Seq(
          FirstElement("study_code", col("study_code"))
          //          SimpleAggregation(name = TRANSMISSIONS, c = col(TRANSMISSION_MODE)), ----> Removed in SNV for performance
        )
      ),
      FrequencySplit(
        "internal_frequencies_wgs",
        filter = Some(col("source") === "WGS"),
        byAffected = false
      )
    )
  )

  def main(args: Array[String]): Unit = ParserForMethods(this).runOrThrow(args, allowPositional = true)

}
