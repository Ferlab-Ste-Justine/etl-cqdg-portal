package bio.ferlab.etl.normalize

import bio.ferlab.datalake.commons.config.DatasetConf
import bio.ferlab.etl.normalize.model.{GENOTYPES, NormalizedConsequences, VCF_SNV_INPUT}
import org.apache.spark.sql.DataFrame
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ConsequencesSpec extends AnyFlatSpec with Matchers with WithSparkSession with WithTestConfig{
  import spark.implicits._

  val raw_variant_calling: DatasetConf = conf.getDataset("raw_vcf")

  val data: Map[String, DataFrame] = Map(
    raw_variant_calling.id -> Seq(VCF_SNV_INPUT(
      `genotypes` = List(
        GENOTYPES(),
        GENOTYPES(`sampleId` = "S20279", `calls` = List(0, 0)),
        GENOTYPES(`sampleId` = "S20280"))
    )).toDF(),
  )


  it should "generate normalized consequences from input VCF" in {
    val results = new Consequences("STU0000001").transform(data)

    val result = results("normalized_consequences").as[NormalizedConsequences].collect()

    result shouldEqual Seq(NormalizedConsequences())
  }
}

