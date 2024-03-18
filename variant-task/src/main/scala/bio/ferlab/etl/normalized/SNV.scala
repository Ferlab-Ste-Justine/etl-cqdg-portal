package bio.ferlab.etl.normalized

import bio.ferlab.datalake.commons.config.{DatasetConf, RuntimeETLContext}
import bio.ferlab.datalake.spark3.etl.v4.SimpleSingleETL
import bio.ferlab.datalake.spark3.implicits.DatasetConfImplicits._
import bio.ferlab.datalake.spark3.implicits.GenomicImplicits._
import bio.ferlab.datalake.spark3.implicits.GenomicImplicits.columns._
import bio.ferlab.etl.normalized.SNV._
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types.StringType
import org.apache.spark.sql.{DataFrame, SparkSession}

import java.time.LocalDateTime

case class SNV(rc: RuntimeETLContext, studyId: String, studyCode: String, owner: String, dataset: String, batch: String, releaseId: String, referenceGenomePath: Option[String]) extends SimpleSingleETL(rc) {

  private val enriched_specimen: DatasetConf = conf.getDataset("enriched_specimen")
  private val raw_variant_calling: DatasetConf = conf.getDataset("raw_vcf")
  private val normalized_task: DatasetConf = conf.getDataset("normalized_task")
  override val mainDestination: DatasetConf = conf.getDataset("normalized_snv")

  override def extract(lastRunDateTime: LocalDateTime = minValue,
                       currentRunDateTime: LocalDateTime = LocalDateTime.now()): Map[String, DataFrame] = {

    Map(
      "raw_vcf" -> vcf(raw_variant_calling.location
        .replace("{{STUDY_CODE}}", s"$studyCode")
        .replace("{{DATASET}}", s"$dataset")
        .replace("{{BATCH}}", s"$batch")
        .replace("{{OWNER}}", s"$owner"), referenceGenomePath = None)
        .where(col("contigName").isin(validContigNames: _*)),
      enriched_specimen.id -> enriched_specimen.read.where(col("study_id") === studyId),
      normalized_task.id -> normalized_task.read.where(col("study_id") === studyId && col("release_id") === releaseId),
    )

  }

  override def transformSingle(data: Map[String, DataFrame], lastRunDateTime: LocalDateTime, currentRunDateTime: LocalDateTime): DataFrame = {
    val enrichedSpecimenDF = data(enriched_specimen.id).select("sample_id", "is_affected", "participant_id", "family_id", "sex", "mother_id", "father_id", "study_id", "study_code")
      .withColumnRenamed("is_affected", "affected_status")

    val occurrences = selectOccurrences(data("raw_vcf"), releaseId, dataset, batch)
    occurrences.join(broadcast(enrichedSpecimenDF), Seq("sample_id"))
      .withAlleleDepths()
      .withSource(data(normalized_task.id))
  }

  override def replaceWhere: Option[String] = Some(s"study_id = '$studyId' and dataset='$dataset' and batch='$batch' ")

}

object SNV {
  private final val GENES_SYMBOL = "genes_symbol"

  implicit class DataFrameOps(df: DataFrame) {
    def withSource(task: DataFrame)(implicit spark: SparkSession): DataFrame = {
      import spark.implicits._
      val taskDf = task
        .select($"ldm_sample_id" as "sample_id",
          $"experimental_strategy" as "source")

      df.join(broadcast(taskDf), Seq("sample_id"), "left")
    }
  }

  private def selectOccurrences(inputDF: DataFrame, releaseId: String, dataset: String, batch: String): DataFrame = {
    val inputDfExpGenotypes = inputDF
      .filter(col("INFO_FILTERS").isNull || array_contains(col("INFO_FILTERS"), "PASS")) // Remove low quality variants
      .withColumn("annotation", firstCsq)
      .withColumn("hgvsg", hgvsg)
      .withColumn("variant_class", variant_class)
      .withColumn(GENES_SYMBOL, array_distinct(csq("symbol")))
      .drop("annotation", "INFO_CSQ")
      .withColumn("genotype", explode(col("genotypes")))
      .drop("genotypes")

    inputDfExpGenotypes
      .withColumn(
        "genotype",
        struct(
          col("genotype.sampleId"),
          col("genotype.conditionalQuality"),
          col("genotype.SB"),
          col("genotype.alleleDepths"),
          col("genotype.phased"),
          col("genotype.calls"),
          col("genotype.MIN_DP"),
          col("genotype.phredLikelihoods"),
          col("genotype.depth"),
          optional_info(inputDfExpGenotypes, "genotype.RGQ", "RGQ", "int"),
          optional_info(inputDfExpGenotypes, "genotype.PGT", "PGT", "string"),

        )
      )

    val occurrences = inputDfExpGenotypes
      .select(
        chromosome,
        start,
        end,
        reference,
        alternate,
        name,
        col(GENES_SYMBOL),
        col("hgvsg"),
        col("variant_class"),
        col("genotype.sampleId") as "sample_id",
        col("genotype.alleleDepths") as "ad",
        col("genotype.depth") as "dp",
        col("genotype.conditionalQuality") as "gq",
        col("genotype.calls") as "calls",
        array_contains(col("genotype.calls"), 1) as "has_alt",
        col("qual") as "quality",
        col("INFO_FILTERS")(0) as "filter",
        ac as "info_ac",
        an as "info_an",
        af as "info_af",
        col("INFO_END") as "info_end",
        optional_info(inputDF, "INFO_OLD_RECORD", "info_old_record", "string"),
        optional_info(inputDF, "INFO_BaseQRankSum", "info_baseq_rank_sum", "double"),
        optional_info(inputDF, "INFO_ExcessHet", "info_excess_het", "double"),
        optional_info(inputDF, "INFO_FS", "info_fs", "double"),
        optional_info(inputDF, "INFO_DS", "info_ds", "boolean"),
        optional_info(inputDF, "INFO_FractionInformativeReads", "info_fraction_informative_reads", "double"),
        optional_info(inputDF, "INFO_InbreedingCoeff", "info_inbreed_coeff", "double"),
        optional_info(inputDF, "INFO_MLEAC", "info_mleac", "array<int>"),
        optional_info(inputDF, "INFO_MLEAF", "info_mleaf", "array<double>"),
        optional_info(inputDF, "INFO_MQ", "info_mq", "double"),
        optional_info(inputDF, "INFO_MQRankSum", "info_m_qrank_sum", "double"),
        optional_info(inputDF, "INFO_QD", "info_qd", "double"),
        optional_info(inputDF, "INFO_R2_5P_bias", "info_r2_5p_bias", "double"),
        optional_info(inputDF, "INFO_ReadPosRankSum", "info_read_pos_rank_sum", "double"),
        optional_info(inputDF, "INFO_SOR", "info_sor", "double"),
        optional_info(inputDF, "INFO_VQSLOD", "info_vqslod", "double"),
        optional_info(inputDF, "INFO_culprit", "info_culprit", "string"),
        optional_info(inputDF, "INFO_DP", "info_dp", "int"),
        optional_info(inputDF, "INFO_HaplotypeScore", "info_haplotype_score", "double"),
        lit(releaseId) as "release_id",
        lit(dataset) as "dataset",
        lit(batch) as "batch"
      )
      .withColumn("is_normalized",col("info_old_record").isNotNull)
      .withColumn("is_multi_allelic", lit(false)) //we dont split multi allelics with glow anymore. In the future we should try to infer this field from info_old_record
      .withColumn("old_multi_allelic", lit(null).cast(StringType)) //Should be deleted in the future, replace by info_old_record
      .drop("annotation")
      .withColumn("zygosity", zygosity(col("calls")))
    occurrences
  }

}