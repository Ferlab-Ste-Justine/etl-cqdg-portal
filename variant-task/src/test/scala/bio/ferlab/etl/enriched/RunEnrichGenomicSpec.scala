package bio.ferlab.etl.enriched

import bio.ferlab.datalake.commons.config.DatasetConf
import bio.ferlab.datalake.spark3.genomics.enriched.Variants
import bio.ferlab.datalake.testutils.models.enriched._
import bio.ferlab.datalake.testutils.models.normalized._
import bio.ferlab.datalake.testutils.{SparkSpec, TestETLContext}
import bio.ferlab.etl.WithTestConfig
import bio.ferlab.etl.model._
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.functions.{explode, lit}

class RunEnrichGenomicSpec extends SparkSpec with WithTestConfig {

  import spark.implicits._

  val thousand_genomes: DatasetConf = conf.getDataset("normalized_1000_genomes")
  val normalized_snv: DatasetConf = conf.getDataset("normalized_snv")
  val topmed_bravo: DatasetConf = conf.getDataset("normalized_topmed_bravo")
  val gnomad_constraint: DatasetConf = conf.getDataset("normalized_gnomad_constraint_v2_1_1")
  val gnomad_genomes_v2_1_1: DatasetConf = conf.getDataset("normalized_gnomad_genomes_v2_1_1")
  val gnomad_exomes_v2_1_1: DatasetConf = conf.getDataset("normalized_gnomad_exomes_v2_1_1")
  val gnomad_genomes_v3: DatasetConf = conf.getDataset("normalized_gnomad_genomes_v3")
  val dbsnp: DatasetConf = conf.getDataset("normalized_dbsnp")
  val clinvar: DatasetConf = conf.getDataset("normalized_clinvar")
  val genes: DatasetConf = conf.getDataset("enriched_genes")
  val spliceai: DatasetConf = conf.getDataset("enriched_spliceai")
  val cosmic: DatasetConf = conf.getDataset("normalized_cosmic_mutation_set")

  val occurrencesDf: DataFrame = Seq(
    // At least 10 participants so participant_ids should be shown except the WXS source
    NORMALIZED_SNV(`participant_id` = "PA0001", study_id = "S1", study_code = "study_code1", `source` = "WGS"),
    NORMALIZED_SNV(`participant_id` = "PA0002", study_id = "S1", study_code = "study_code1", `source` = "WGS"),
    NORMALIZED_SNV(`participant_id` = "PA0003", study_id = "S1", study_code = "study_code1", `source` = "WGS"),
    NORMALIZED_SNV(`participant_id` = "PA0004", study_id = "S1", study_code = "study_code1", `source` = "WGS"),
    NORMALIZED_SNV(`participant_id` = "PA0005", study_id = "S1", study_code = "study_code1", `source` = "WGS"),
    NORMALIZED_SNV(`participant_id` = "PA0006", study_id = "S1", study_code = "study_code1", `source` = "WGS"),
    NORMALIZED_SNV(`participant_id` = "PA0007", study_id = "S1", study_code = "study_code1", `source` = "WGS"),
    NORMALIZED_SNV(`participant_id` = "PA0008", study_id = "S1", study_code = "study_code1", `source` = "WGS"),
    NORMALIZED_SNV(`participant_id` = "PA0009", study_id = "S1", study_code = "study_code1", `source` = "WGS"),
    NORMALIZED_SNV(`participant_id` = "PA0010", study_id = "S1", study_code = "study_code1", `source` = "WGS"),
    NORMALIZED_SNV(
      `participant_id` = "PA0011",
      study_id = "S1",
      study_code = "study_code1",
      `source` = "WXS",
      `zygosity` = "HOM"
    ),

    // WXS occurrence should be added to the source and studies lists
    NORMALIZED_SNV(
      `participant_id` = "PA0012",
      study_id = "S2",
      `study_code` = "study_code2",
      `source` = "WXS",
      `zygosity` = "HOM",
      `calls` = List(0, 0)
    ),

    // Study is CAG so participant_ids won't be included in frequencies
    NORMALIZED_SNV(`participant_id` = "PA0013", study_id = "S3", `study_code` = "CAG", `source` = "WGS"),
    NORMALIZED_SNV(`participant_id` = "PA0014", study_id = "S3", `study_code` = "CAG", `source` = "WGS"),
    NORMALIZED_SNV(`participant_id` = "PA0015", study_id = "S3", `study_code` = "CAG", `source` = "WGS"),
    NORMALIZED_SNV(`participant_id` = "PA0016", study_id = "S3", `study_code` = "CAG", `source` = "WGS"),
    NORMALIZED_SNV(`participant_id` = "PA0017", study_id = "S3", `study_code` = "CAG", `source` = "WGS"),
    NORMALIZED_SNV(`participant_id` = "PA0018", study_id = "S3", `study_code` = "CAG", `source` = "WGS"),
    NORMALIZED_SNV(`participant_id` = "PA0019", study_id = "S3", `study_code` = "CAG", `source` = "WGS"),
    NORMALIZED_SNV(`participant_id` = "PA0020", study_id = "S3", `study_code` = "CAG", `source` = "WGS"),
    NORMALIZED_SNV(`participant_id` = "PA0021", study_id = "S3", `study_code` = "CAG", `source` = "WGS"),
    NORMALIZED_SNV(`participant_id` = "PA0022", study_id = "S3", `study_code` = "CAG", `source` = "WGS")
  ).toDF
  val genomesDf: DataFrame = Seq(NormalizedOneKGenomes()).toDF
  val topmed_bravoDf: DataFrame = Seq(NormalizedTopmed()).toDF
  val gnomad_genomes_2_1_1Df: DataFrame = Seq(NormalizedGnomadGenomes211()).toDF
  val gnomad_exomes_2_1_1Df: DataFrame = Seq(NormalizedGnomadExomes211()).toDF
  val gnomad_genomes_3Df: DataFrame = Seq(NormalizedGnomadGenomes3()).toDF
  val dbsnpDf: DataFrame = Seq(NormalizedDbsnp()).toDF
  val clinvarDf: DataFrame = Seq(
    NormalizedClinvar(chromosome = "1", start = 69897, reference = "T", alternate = "C")
  ).toDF
  val genesDf: DataFrame = Seq(EnrichedGenes()).toDF()
  val spliceaiDf: DataFrame = Seq(
    EnrichedSpliceAi(
      chromosome = "1",
      start = 69897,
      reference = "T",
      alternate = "C",
      symbol = "OR4F5",
      ds_ag = 0.01,
      `max_score` = MAX_SCORE(ds = 0.01, `type` = Seq("AG"))
    )
  ).toDF()
  val cosmicDf: DataFrame =
    Seq(NormalizedCosmicMutationSet(chromosome = "1", start = 69897, reference = "T", alternate = "C")).toDF()

  val variantsETL: Variants = RunEnrichGenomic.runVariants(TestETLContext())

  val data = Map(
    normalized_snv.id -> occurrencesDf,
    thousand_genomes.id -> genomesDf,
    topmed_bravo.id -> topmed_bravoDf,
    gnomad_genomes_v2_1_1.id -> gnomad_genomes_2_1_1Df,
    gnomad_exomes_v2_1_1.id -> gnomad_exomes_2_1_1Df,
    gnomad_genomes_v3.id -> gnomad_genomes_3Df,
    dbsnp.id -> dbsnpDf,
    clinvar.id -> clinvarDf,
    genes.id -> genesDf,
    spliceai.id -> spliceaiDf,
    cosmic.id -> cosmicDf
  )

  "variants ETL" should "collect all sources" in {
    val result = variantsETL.transformSingle(data)
    result
      .select("sources")
      .as[Set[String]]
      .head() should contain theSameElementsAs Set("WGS", "WXS")
  }

  it should "collect all studies" in {
    val result = variantsETL
      .transformSingle(data)
      .select(explode($"studies") as "studies")
      .select("studies.*")
      .as[STUDY]
      .collect()

    val expected = Seq(
      STUDY(`study_id` = "S1", `study_code` = "study_code1", `zygosity` = Set("HET", "HOM")),
      STUDY(`study_id` = "S2", `study_code` = "study_code2", `zygosity` = Set("HOM")),
      STUDY(`study_id` = "S3", `study_code` = "CAG", `zygosity` = Set("HET"))
    )

    result should contain theSameElementsAs expected
  }

  it should "only compute frequencies for whole genomes" in {
    val result = variantsETL.transformSingle(data)

    val studyFreqWgs = result
      .select(explode($"study_frequencies_wgs") as "study_frequencies_wgs")
      .select("study_frequencies_wgs.*")
      .as[STUDY_FREQUENCIES_WGS_WITHOUT_TRANSMISSION]
      .collect()

    val internalFreqWgs = result
      .select($"internal_frequencies_wgs.*")
      .as[INTERNAL_FREQUENCIES_WGS]
      .collect()

    studyFreqWgs should contain theSameElementsAs Seq(
      STUDY_FREQUENCIES_WGS_WITHOUT_TRANSMISSION(
        study_id = "S1",
        total = TOTAL(ac = 10, an = 20, pc = 10, pn = 10, hom = 0, af = 0.5, pf = 1.0),
        //        transmission = Set("autosomal_dominant"),
        study_code = "study_code1"
      ),
      STUDY_FREQUENCIES_WGS_WITHOUT_TRANSMISSION(
        study_id = "S3",
        total = TOTAL(ac = 10, an = 20, pc = 10, pn = 10, hom = 0, af = 0.5, pf = 1.0),
        //        transmission = Set("autosomal_dominant"),
        study_code = "CAG"
      )
    )

    internalFreqWgs should contain theSameElementsAs Seq(
      INTERNAL_FREQUENCIES_WGS(total = TOTAL(ac = 20, an = 40, pc = 20, pn = 20, hom = 0, af = 0.5, pf = 1.0))
    )
  }

  it should "return all variants even when only WXS" in {
    val onlyWxsDf = occurrencesDf
      .withColumn("source", lit("WXS"))
    val updatedData = data.updated(normalized_snv.id, onlyWxsDf)

    val result = variantsETL.transformSingle(updatedData)

    result.isEmpty shouldBe false

    result
      .where($"study_frequencies_wgs".isNotNull)
      .collect() shouldBe empty

    result
      .where($"internal_frequencies_wgs".isNotNull)
      .collect() shouldBe empty

    result
      .select(explode($"studies") as "studies")
      .select("studies.*")
      .as[STUDY]
      .collect()
      .length shouldBe 3
  }

}
