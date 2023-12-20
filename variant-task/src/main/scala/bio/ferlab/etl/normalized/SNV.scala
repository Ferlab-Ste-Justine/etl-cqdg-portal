package bio.ferlab.etl.normalized

import bio.ferlab.datalake.commons.config.{DatasetConf, RuntimeETLContext}
import bio.ferlab.datalake.spark3.etl.v4.SimpleSingleETL
import bio.ferlab.datalake.spark3.implicits.DatasetConfImplicits._
import bio.ferlab.datalake.spark3.implicits.GenomicImplicits._
import bio.ferlab.datalake.spark3.implicits.GenomicImplicits.columns._
import bio.ferlab.etl.Constants.columns.GENES_SYMBOL
import bio.ferlab.etl.normalized.SNV._
import org.apache.spark.sql.functions._
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

    val vcf = getSNV(data("raw_vcf"))
    val enrichedSpecimenDF = data(enriched_specimen.id).select("sample_id", "is_affected", "participant_id", "family_id", "gender", "mother_id", "father_id", "study_id", "study_code")
      .withColumnRenamed("is_affected", "affected_status")

    val occurrences = selectOccurrences(vcf, releaseId, dataset, batch)
    occurrences.join(broadcast(enrichedSpecimenDF), Seq("sample_id"))
      .withAlleleDepths()
      .withSource(data(normalized_task.id))
  }

  override def replaceWhere: Option[String] = Some(s"study_id = '$studyId' and dataset='$dataset' and batch='$batch' ")

}

object SNV {
  implicit class DataFrameOps(df: DataFrame) {
    def withSource(task: DataFrame)(implicit spark: SparkSession): DataFrame = {
      import spark.implicits._
      val taskDf = task
        .select($"ldm_sample_id" as "sample_id",
          $"experimental_strategy" as "source")

      df.join(broadcast(taskDf), Seq("sample_id"), "left")
    }
  }

  private def selectOccurrences(inputDF: DataFrame, releaseId: String, dataset: String, batch:String): DataFrame = {
    val occurrences = inputDF
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
        is_multi_allelic,
        old_multi_allelic,
        col("qual") as "quality",
        col("INFO_FILTERS")(0) as "filter",
        ac as "info_ac",
        an as "info_an",
        af as "info_af",
        col("INFO_SOR") as "info_sor",
        col("INFO_ReadPosRankSum") as "info_read_pos_rank_sum",
        col("INFO_FS") as "info_fs",
        col("INFO_DP") as "info_dp",
        optional_info(inputDF, "INFO_DS", "info_ds", "boolean"),
        col("INFO_MQ") as "info_mq",
        col("INFO_QD") as "info_qd",
        col("INFO_END") as "info_end",
        col("INFO_MQRankSum") as "info_m_qrank_sum",
        optional_info(inputDF, "INFO_HaplotypeScore", "info_haplotype_score", "float"),
        //        col("file_name"),
        lit(releaseId) as "release_id",
        lit(dataset) as "dataset",
        lit(batch) as "batch",
        is_normalized
      )
      .drop("annotation")
      .withColumn("zygosity", zygosity(col("calls")))
    occurrences
  }

  def getSNV(inputDF: DataFrame): DataFrame = {
    val inputDfExpGenotypes = inputDF
      .filter(array_contains(col("INFO_FILTERS"), "PASS")) // Remove low quality variants
      .withColumn("annotation", firstCsq)
      .withColumn("hgvsg", hgvsg)
      .withColumn("variant_class", variant_class)
      .withColumn(GENES_SYMBOL, array_distinct(csq("symbol")))
      .drop("annotation", "INFO_CSQ")
      .withColumn("INFO_DS", lit(null).cast("boolean"))
      .withColumn("INFO_HaplotypeScore", lit(null).cast("double"))
      .withColumn("genotype", explode(col("genotypes")))
      .drop("genotypes")

    inputDfExpGenotypes
      .withColumn(
        "genotype",
        struct(
          col("genotype.sampleId"),
          col("genotype.conditionalQuality"),
          //          col("genotype.SQ"),
          //          col("genotype.PRI"),
          //          col("genotype.posteriorProbabilities"),
          col("genotype.SB"),
          col("genotype.alleleDepths"),
          //          col("genotype.ICNT"),
          //          col("genotype.AF"),
          col("genotype.phased"),
          col("genotype.calls"),
          col("genotype.MIN_DP"),
          col("genotype.phredLikelihoods"),
          col("genotype.depth"),
          optional_info(inputDfExpGenotypes, "genotype.RGQ", "RGQ", "int"),
          optional_info(inputDfExpGenotypes, "genotype.PGT", "PGT", "string"),
          //          col("genotype.SPL"),
          //          col("genotype.PS"),
          //          col("genotype.MB"), //TODO confirm is ok
        )
      )
  }

}