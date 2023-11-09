package bio.ferlab.etl.normalized

import bio.ferlab.datalake.commons.config.DatasetConf
import bio.ferlab.datalake.testutils.{SparkSpec, TestETLContext}
import bio.ferlab.etl.WithTestConfig
import bio.ferlab.etl.model.{GENOTYPES, NormalizedConsequences, VCF_SNV_INPUT}
import org.apache.spark.sql.DataFrame

class ConsequencesSpec extends SparkSpec with WithTestConfig {

  import spark.implicits._

  val raw_variant_calling: DatasetConf = conf.getDataset("raw_vcf")

  val data: Map[String, DataFrame] = Map(
    raw_variant_calling.id -> Seq(
      VCF_SNV_INPUT(`contigName` = "chr1", `INFO_FILTERS` = Seq("PASS"),
        `genotypes` = List(
          GENOTYPES(),
          GENOTYPES(`sampleId` = "S20279", `calls` = List(0, 0)),
          GENOTYPES(`sampleId` = "S20280"))),
      VCF_SNV_INPUT(`contigName` = "chr2", `INFO_FILTERS` = Seq("DRAGENSnpHardQUAL"),
        `genotypes` = List(GENOTYPES(`sampleId` = "S20281"))) // Should be filtered out
    ).toDF(),
  )


  it should "generate normalized consequences from input VCF" in {
    val results = Consequences(TestETLContext(), "STU0000001", "STU0000001", "owner", "dataset_default", "annotated_vcf", None).transform(data)

    val result = results("normalized_consequences").as[NormalizedConsequences].collect()

    result.filter(r => r.`chromosome` == "2") shouldBe empty // Low quality variant
    result should contain theSameElementsAs Seq(NormalizedConsequences())
  }
}

