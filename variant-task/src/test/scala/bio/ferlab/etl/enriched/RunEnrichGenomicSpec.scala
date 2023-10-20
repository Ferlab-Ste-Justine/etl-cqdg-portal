package bio.ferlab.etl.enriched

import bio.ferlab.datalake.commons.config.DatasetConf
import bio.ferlab.datalake.spark3.genomics.enriched.Variants
import bio.ferlab.datalake.testutils.models.enriched._
import bio.ferlab.datalake.testutils.models.frequency.{FrequencyByStudyId, GlobalFrequency}
import bio.ferlab.datalake.testutils.models.normalized._
import bio.ferlab.datalake.testutils.{SparkSpec, TestETLContext}
import bio.ferlab.etl.WithTestConfig
import bio.ferlab.etl.model._
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.functions.explode

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
    NORMALIZED_SNV(`participant_id` = "PA0011", study_id = "S1", study_code = "study_code1", `source` = "WXS"),

    // Source WXS should be added to the source list
    NORMALIZED_SNV(`participant_id` = "PA0012", study_id = "S2", `study_code` = "study_code2", `zygosity` = "WT", `calls` = List(0, 0), has_alt = false),

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
    NORMALIZED_SNV(`participant_id` = "PA0022", study_id = "S3", `study_code` = "CAG", `source` = "WGS"),
  ).toDF
  val genomesDf: DataFrame = Seq(NormalizedOneKGenomes()).toDF
  val topmed_bravoDf: DataFrame = Seq(NormalizedTopmed()).toDF
  val gnomad_genomes_2_1_1Df: DataFrame = Seq(NormalizedGnomadGenomes211()).toDF
  val gnomad_exomes_2_1_1Df: DataFrame = Seq(NormalizedGnomadExomes211()).toDF
  val gnomad_genomes_3Df: DataFrame = Seq(NormalizedGnomadGenomes3()).toDF
  val dbsnpDf: DataFrame = Seq(NormalizedDbsnp()).toDF
  val clinvarDf: DataFrame = Seq(NormalizedClinvar(chromosome = "1", start = 69897, reference = "T", alternate = "C")).toDF
  val genesDf: DataFrame = Seq(EnrichedGenes()).toDF()
  val spliceaiDf: DataFrame = Seq(EnrichedSpliceAi(chromosome = "1", start = 69897, reference = "T", alternate = "C", symbol = "OR4F5", ds_ag = 0.01, `max_score` = MAX_SCORE(ds = 0.01, `type` = Seq("AG")))).toDF()
  val cosmicDf: DataFrame = Seq(NormalizedCosmicMutationSet(chromosome = "1", start = 69897, reference = "T", alternate = "C")).toDF()

  val variantsETL: Variants = RunEnrichGenomic.variants(TestETLContext())

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

  "variants ETL" should "only compute frequencies for whole genomes" in {
    val result = variantsETL.transformSingle(data)

    result.show(false)

    val studiesFreq = result
      .select(explode($"studies") as "studies")
      .select("studies.*")
      .as[STUDY]
      .collect()

    val internalFreqWgs = result
      .select($"internal_frequencies_wgs.*")
      .as[INTERNAL_FREQUENCIES_WGS]
      .collect()

    studiesFreq should contain theSameElementsAs Seq(
      STUDY(study_id = "S1",
        total = TOTAL(ac = 10, an = 20, pc = 10, pn = 10, hom = 0, af = 0.5, pf = 1.0),
        participant_ids = Set("PA0001", "PA0002", "PA0003", "PA0004", "PA0005", "PA0006", "PA0007", "PA0008", "PA0009", "PA0010"),
        transmission = Set("autosomal_dominant"),
        zygosity = Set("HET"),
        study_code = "study_code1"
      ),
      STUDY(study_id = "S3",
        total = TOTAL(ac = 10, an = 20, pc = 10, pn = 10, hom = 0, af = 0.5, pf = 1.0),
        participant_ids = null,
        transmission = Set("autosomal_dominant"),
        zygosity = Set("HET"),
        study_code = "CAG"
      )
    )

    internalFreqWgs should contain theSameElementsAs Seq(
      INTERNAL_FREQUENCIES_WGS(total = TOTAL(ac = 20, an = 40, pc = 20, pn = 20, hom = 0, af = 0.5, pf = 1.0))
    )
  }

}
