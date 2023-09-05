package bio.ferlab.etl.normalized

import bio.ferlab.datalake.commons.config.{DatasetConf, RuntimeETLContext}
import bio.ferlab.datalake.spark3.etl.v3.SimpleSingleETL
import bio.ferlab.datalake.spark3.implicits.DatasetConfImplicits._
import bio.ferlab.datalake.spark3.implicits.GenomicImplicits._
import bio.ferlab.datalake.spark3.implicits.GenomicImplicits.columns._
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.functions._

import java.time.LocalDateTime

case class SNV(rc:RuntimeETLContext, studyId: String, releaseId: String, vcfPattern: String, referenceGenomePath: Option[String]) extends SimpleSingleETL(rc) {
  private val enriched_specimen: DatasetConf = conf.getDataset("enriched_specimen")
  private val raw_variant_calling: DatasetConf = conf.getDataset("raw_vcf")
  override val mainDestination: DatasetConf = conf.getDataset("normalized_snv")

  override def extract(lastRunDateTime: LocalDateTime = minDateTime,
                       currentRunDateTime: LocalDateTime = LocalDateTime.now()): Map[String, DataFrame] = {

    Map(
      "raw_vcf" -> vcf(raw_variant_calling.location.replace("{{STUDY_ID}}", s"${studyId}_test"), referenceGenomePath = None) //TODO remove "test" when server is available
        .where(col("contigName").isin(validContigNames: _*)),
      enriched_specimen.id -> enriched_specimen.read.where(col("study_id") === studyId)
    )

  }

  override def transformSingle(data: Map[String, DataFrame], lastRunDateTime: LocalDateTime, currentRunDateTime: LocalDateTime): DataFrame = {

    val vcf = getSNV(data("raw_vcf"))
    val enrichedSpecimenDF = data(enriched_specimen.id)

    val occurrences = selectOccurrences(vcf, studyId)

    val columnNames = Seq("gq", "dp", "info_qd", "ad_ref", "ad_alt", "ad_total", "ad_ratio", "calls","affected_status", "zygosity")
    //missing "filters"

    occurrences.join(enrichedSpecimenDF, Seq("sample_id"))
      .withColumn("affected_status", col("participant.is_affected"))
      .withAlleleDepths()
      .withRelativesGenotype(columnNames,
        participantIdColumn = col("participant.participant_id"),
        familyIdColumn = col("participant.family_id")
      )
      .withParentalOrigin("parental_origin", col("calls"), col("father_calls"), col("mother_calls"))
      .withGenotypeTransmission("transmission", `gender_name` = "participant.gender")
//      .withCompoundHeterozygous(patientIdColumnName = "participant.participant_id") //TODO
  }

  override def replaceWhere: Option[String] = Some(s"study_id = '$studyId'")

  private def selectOccurrences(inputDF: DataFrame, studyId: String): DataFrame = {
    val occurrences = inputDF
      .select(
        chromosome,
        start,
        end,
        reference,
        alternate,
        name,
        col("hgvsg"),
        col("variant_class"),
        col("genotype.sampleId") as "sample_id",
        col("genotype.alleleDepths") as "ad",
        col("genotype.depth") as "dp",
        col("genotype.conditionalQuality") as "gq",
        col("genotype.calls") as "calls",
        has_alt,
        is_multi_allelic,
        old_multi_allelic,
        col("qual") as "quality",
        col("INFO_FILTERS")(0) as "filter",
        ac as "info_ac",
        an as "info_an",
        af as "info_af",
        col("INFO_SOR") as "info_sor",
        col("INFO_ReadPosRankSum") as "info_read_pos_rank_sum",
        col("INFO_InbreedingCoeff") as "info_inbreeding_coeff",
        col("INFO_FS") as "info_fs",
        col("INFO_DP") as "info_dp",
        optional_info(inputDF, "INFO_DS", "info_ds", "boolean"),
        col("INFO_BaseQRankSum") as "info_base_qrank_sum",
        col("INFO_MLEAF")(0) as "info_mleaf",
        col("INFO_MLEAC")(0) as "info_mleac",
        col("INFO_MQ") as "info_mq",
        col("INFO_QD") as "info_qd",
        col("INFO_END") as "info_end",
        col("INFO_RAW_MQ") as "info_raw_mq",
        col("INFO_culprit") as "info_culprit",
        col("INFO_ClippingRankSum") as "info_clipping_rank_sum",
        col("INFO_NEGATIVE_TRAIN_SITE") as "info_info_negative_train_site",
        col("INFO_POSITIVE_TRAIN_SITE") as "info_info_positive_train_site",
        col("INFO_VQSLOD") as "info_vqslod",
        col("INFO_MQRankSum") as "info_m_qrank_sum",
        col("INFO_ExcessHet") as "info_excess_het",
        col("INFO_OLD_MULTIALLELIC") as "info_old_multiallelic",
        optional_info(inputDF, "INFO_HaplotypeScore", "info_haplotype_score", "float"),
        //        col("file_name"),
        lit(releaseId) as "releaseId",
        is_normalized
      )
      .drop("annotation")
      .withColumn("zygosity", zygosity(col("calls")))
    occurrences
  }

  def getSNV(inputDF: DataFrame): DataFrame = {
    inputDF
      .withColumn("annotation", firstCsq)
      .withColumn("hgvsg", hgvsg)
      .withColumn("variant_class", variant_class)
      .drop("annotation", "INFO_CSQ")
      .withColumn("INFO_DS", lit(null).cast("boolean"))
      .withColumn("INFO_HaplotypeScore", lit(null).cast("double"))
      .withColumn("genotype", explode(col("genotypes")))
      .drop("genotypes")
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
          col("genotype.RGQ"),
          col("genotype.PGT"),
          //          col("genotype.SPL"),
          //          col("genotype.PS"),
          //          col("genotype.MB"), //TODO confirm is ok
        )
      )
  }

}