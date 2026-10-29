import bio.ferlab.datalake.spark3.transformation.Transformation
import bio.ferlab.fhir.etl.transformations.Transformations.observationCauseOfDeathMappings
import models._
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class CauseOfDeathMappingsSpec extends AnyFlatSpec with Matchers with WithSparkSession with BeforeAndAfterAll {

  import spark.implicits._

  // ImportTaskSpec runs in the same JVM and needs its own session (Delta, S3 endpoint): static configs
  // are ignored if getOrCreate() returns this one, so stop it once this suite is done.
  override def afterAll(): Unit = {
    spark.stop()
    super.afterAll()
  }

  "observationCauseOfDeathMappings" should "keep an Observation coded with the cause-of-death CodeSystem" in {
    val input = Seq(OBSERVATION_INPUT()).toDF()

    val output = Transformation.applyTransformations(input, observationCauseOfDeathMappings)

    output.as[NORMALIZED_CAUSE_OF_DEATH].collect() should contain theSameElementsAs Seq(NORMALIZED_CAUSE_OF_DEATH())
  }

  it should "not feed cause_of_death from an Observation coded with another CodeSystem" in {
    val input = Seq(
      OBSERVATION_INPUT(
        `code` = CODEABLE_CONCEPT_INPUT(
          Seq(CODING_INPUT(`system` = "https://fhir.cqdg.ca/CodeSystem/cqdg-observation-code", `code` = "Phenotype"))
        )
      ),
      // Previous (wrong) URL used by the ETL before CQDG-1564
      OBSERVATION_INPUT(
        `fhir_id` = "PRT0000002",
        `code` = CODEABLE_CONCEPT_INPUT(
          Seq(CODING_INPUT(`system` = "https://fhir.cqdg.ferlab.bio/CodeSystem/cause-of-death"))
        ),
        `subject` = REFERENCE_INPUT("Patient/PRT0000002")
      )
    ).toDF()

    val output = Transformation.applyTransformations(input, observationCauseOfDeathMappings)

    output.as[NORMALIZED_CAUSE_OF_DEATH].collect() shouldBe empty
  }
}
