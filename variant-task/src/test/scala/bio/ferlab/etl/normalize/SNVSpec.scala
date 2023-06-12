package bio.ferlab.etl.normalize

import bio.ferlab.datalake.commons.config.DatasetConf
import bio.ferlab.etl.normalize.model._
import org.apache.spark.sql.DataFrame
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class SNVSpec extends AnyFlatSpec with Matchers with WithSparkSession with WithTestConfig{
  import spark.implicits._

  val raw_variant_calling: DatasetConf = conf.getDataset("raw_vcf")
  val specimenEnriched: DatasetConf = conf.getDataset("enriched_specimen")

  val specimenEnrichedDf: DataFrame = Seq(
    SPECIMEN_ENRICHED(`participant` = PARTICIPANT_ENRICHED(`participant_id` = "P1", `relationship_to_proband` = "is_proband", `is_affected` = true), `sample_id` = "S1"),
    SPECIMEN_ENRICHED(`participant` = PARTICIPANT_ENRICHED(`participant_id` = "P2", `relationship_to_proband` = "father"), `sample_id` = "S2"),
    SPECIMEN_ENRICHED(`participant` = PARTICIPANT_ENRICHED(`participant_id` = "P3", `relationship_to_proband` = "mother", `is_affected` = true), `sample_id` = "S3")
  ).toDF


  val data: Map[String, DataFrame] = Map(
    raw_variant_calling.id -> Seq(VCF_SNV_INPUT(
      `genotypes` = List(
        GENOTYPES(),
        GENOTYPES(`sampleId` = "S20279", `calls` = List(0, 0)),
        GENOTYPES(`sampleId` = "S20280"))
    )).toDF(),
    specimenEnriched.id -> specimenEnrichedDf,
  )

  it should "generate NormalizedSNV from input raw VCF" in {
    val dataFomVCFFile: Map[String, DataFrame] = Map(
//      raw_variant_calling.id -> vcf(List("variant-task/src/test/resources/S03329.vep.vcf"), None),
      raw_variant_calling.id -> Seq(VCF_SNV_INPUT(`genotypes` = List(
        GENOTYPES(`sampleId` = "S1"),
        GENOTYPES(`sampleId` = "S2", `calls` = List(0, 0)),
        GENOTYPES(`sampleId` = "S3")))).toDF(),
      specimenEnriched.id -> specimenEnrichedDf,
    )

    val results = new SNV("STU0000001").transform(dataFomVCFFile)

    val result = results("normalized_snv").as[NormalizedSNV].collect()

    result.filter(e => e.`sample_id` === "S1").head shouldBe NormalizedSNV()
    result.filter(e => e.`sample_id` === "S2").head shouldBe
      NormalizedSNV(
        `sample_id` = "S2",
        `participant` = PARTICIPANT(`participant_id` = "P2", `relationship_to_proband` = "father", `is_affected` = false),
        `affected_status` = false,
        `calls`= Seq(0, 0),
        `has_alt` = 0,
        `zygosity` = "WT",
        `parental_origin` = null,
        `transmission` = "non_carrier_proband"
      )
    result.filter(e => e.`sample_id` === "S3").head shouldBe
      NormalizedSNV(
        `sample_id` = "S3",
        `participant` = PARTICIPANT(`participant_id` = "P3", `relationship_to_proband` = "mother"),
      )
  }
}

