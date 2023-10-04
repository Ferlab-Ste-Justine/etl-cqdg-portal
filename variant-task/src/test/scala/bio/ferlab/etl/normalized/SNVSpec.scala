package bio.ferlab.etl.normalized

import bio.ferlab.datalake.commons.config.DatasetConf
import bio.ferlab.datalake.testutils.TestETLContext
import bio.ferlab.etl.normalized.model._
import org.apache.spark.sql.DataFrame
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class SNVSpec extends AnyFlatSpec with Matchers with WithSparkSession with WithTestConfig{
  import spark.implicits._

  val raw_variant_calling: DatasetConf = conf.getDataset("raw_vcf")
  val specimenEnriched: DatasetConf = conf.getDataset("enriched_specimen")

  val specimenEnrichedDf: DataFrame = Seq(
    SPECIMEN_ENRICHED(`participant_id` = "P1", `is_affected` = Some(true), `sample_id` = "S1"),
    SPECIMEN_ENRICHED(`participant_id` = "P2", `sample_id` = "S2"),
    SPECIMEN_ENRICHED(`participant_id` = "P3", `is_affected` = Some(true), `sample_id` = "S3")
  ).toDF

  it should "generate NormalizedSNV from input raw VCF" in {
    val dataFomVCFFile: Map[String, DataFrame] = Map(
      raw_variant_calling.id -> Seq(VCF_SNV_INPUT(`genotypes` = List(
        GENOTYPES(`sampleId` = "S1"),
        GENOTYPES(`sampleId` = "S2", `calls` = List(0, 0)),
        GENOTYPES(`sampleId` = "S3")))).toDF(),
      specimenEnriched.id -> specimenEnrichedDf,
    )

    val results = SNV(TestETLContext(),"STU0000001", "owner", "dataset_default", releaseId = "1", vcfPattern = "", None).transform(dataFomVCFFile)

    val result = results("normalized_snv").as[NORMALIZED_SNV].collect()

    result.filter(e => e.`sample_id` === "S1").head shouldBe NORMALIZED_SNV()
    result.filter(e => e.`sample_id` === "S2").head shouldBe
      NORMALIZED_SNV(
        `sample_id` = "S2",
        `participant_id` = "P2",
        `is_affected` = false,
        `affected_status` = false,
        `calls`= Seq(0, 0),
        `has_alt` = false,
        `zygosity` = "WT",
        `parental_origin` = null,
        `transmission_mode` = "non_carrier_proband"
      )
    result.filter(e => e.`sample_id` === "S3").head shouldBe
      NORMALIZED_SNV(
        `sample_id` = "S3",
        `participant_id` = "P3"
      )

//    ClassGenerator.writeClassFile(
//      "bio.ferlab.etl.variant-task.model",
//      "NORMALIZED_SNV",
//      results("normalized_snv"),
//      "variant-task/src/test/scala/")

  }
}

