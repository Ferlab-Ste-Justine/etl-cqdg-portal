package bio.ferlab.etl.enriched

import bio.ferlab.datalake.commons.config.RuntimeETLContext
import bio.ferlab.datalake.spark3.genomics.enriched.{Consequences, Variants}
import bio.ferlab.datalake.spark3.genomics.{FirstElement, FrequencySplit, SimpleAggregation, SimpleSplit}
import mainargs.{ParserForMethods, main}
import org.apache.spark.sql.functions._

object RunEnrichGenomic {

  @main
  def snv(rc: RuntimeETLContext): Unit = variants(rc).run()

  @main
  def consequences(rc: RuntimeETLContext): Unit = Consequences(rc).run()

  @main
  def all(rc: RuntimeETLContext): Unit = {
    snv(rc)
    consequences(rc)
  }

  def variants(rc: RuntimeETLContext): Variants = Variants(
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
          FirstElement("study_code", col("study_code")),
          //          SimpleAggregation(name = TRANSMISSIONS, c = col(TRANSMISSION_MODE)), ----> Removed in SNV for performance
        )
      ),
      FrequencySplit(
        "internal_frequencies_wgs",
        filter = Some(col("source") === "WGS"),
        byAffected = false
      )))

  def main(args: Array[String]): Unit = ParserForMethods(this).runOrThrow(args, allowPositional = true)


}