import bio.ferlab.datalake.commons.config.{Configuration, ConfigurationLoader, SimpleConfiguration}
import bio.ferlab.datalake.spark3.loader.GenericLoader.read
import bio.ferlab.fhir.etl.centricTypes.{ProgramCentric, StudyCentric}
import model._
import model.centric.{PROGRAM_CENTRIC, CONTACT => PROGRAM_CONTACT}
import model.input.LIST_INPUT
import org.apache.spark.sql.DataFrame
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ProgramCentricSpec extends AnyFlatSpec with Matchers with WithSparkSession {

  import spark.implicits._

  implicit val conf: Configuration = ConfigurationLoader.loadFromResources[SimpleConfiguration]("config/dev-cqdg.conf")

  "transform" should "prepare index program_centric" in {
    val data: Map[String, DataFrame] = Map(
      "normalized_list" -> Seq(LIST_INPUT()).toDF(),
    )

    val output = new ProgramCentric()(conf).transform(data)

    output.keys should contain("es_index_program_centric")

    val program_centric = output("es_index_program_centric")

    val programCentricOutput = PROGRAM_CENTRIC(`contacts` = Seq(
      PROGRAM_CONTACT(`name` = null, `institution` = "RI-MUHC", `role_en` = null, `role_fr` = null,
        `picture_url` = null, `email` = "info@rare.quebec", `website` = "https://rare.quebec")
    ))

    program_centric.as[PROGRAM_CENTRIC].collect() should contain theSameElementsAs
      Seq(programCentricOutput)
  }

}
