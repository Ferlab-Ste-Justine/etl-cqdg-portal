package bio.ferlab.etl.enriched

import bio.ferlab.datalake.commons.config.RuntimeETLContext
import bio.ferlab.datalake.spark3.genomics.enriched.{Consequences, Variants}
import bio.ferlab.datalake.spark3.genomics.{FirstElement, FrequencySplit, OccurrenceAggregation, SimpleAggregation}
import bio.ferlab.etl.Constants.columns.{TRANSMISSIONS, TRANSMISSION_MODE}
import mainargs.{ParserForMethods, main}
import org.apache.spark.sql.Column
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
    frequencies = Seq(
      FrequencySplit(
        "studies",
        filter = Some(col("source") === "WGS"), // Only compute frequencies for whole genomes
        splitBy = Some(col("study_id")),
        byAffected = false,
        extraAggregations = Seq(
          FirstElement("study_code", col("study_code")),
//          SimpleAggregation(name = TRANSMISSIONS, c = col(TRANSMISSION_MODE)), ----> Removed in SNV for performance
          SimpleAggregation(name = "zygosity", c = col("zygosity"))
        )
      ),
      FrequencySplit(
        "internal_frequencies_wgs",
        filter = Some(col("source") === "WGS"),
        splitBy = None,
        byAffected = false
      )))

  def main(args: Array[String]): Unit = ParserForMethods(this).runOrThrow(args, allowPositional = true)


}