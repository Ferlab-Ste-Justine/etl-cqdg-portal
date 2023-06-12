import bio.ferlab.datalake.commons.config.{Configuration, ConfigurationLoader, SimpleConfiguration}
import bio.ferlab.datalake.spark3.loader.GenericLoader.read
import bio.ferlab.fhir.etl.centricTypes.SimpleParticipant
import model._
import org.apache.spark.sql.DataFrame
import org.scalatest.matchers.should.Matchers
import org.scalatest.flatspec.AnyFlatSpec
import pureconfig.generic.auto._

class SimpleParticipantSpec extends AnyFlatSpec with Matchers with WithSparkSession {

  import spark.implicits._

  implicit val conf: Configuration = ConfigurationLoader.loadFromResources[SimpleConfiguration]("config/dev-cqdg.conf")

  "transform" should "prepare simple_participant" in {
    val data: Map[String, DataFrame] = Map(
      "normalized_research_study" -> Seq(RESEARCHSTUDY()).toDF(),
      "normalized_patient" -> Seq(
        PATIENT_INPUT(fhir_id = "P1"),
        PATIENT_INPUT(fhir_id = "P2"),
        PATIENT_INPUT(fhir_id = "P3")
      ).toDF(),
      "normalized_family_relationship" -> Seq(
        FAMILY_RELATIONSHIP(),
      ).toDF(),
      "normalized_phenotype" -> Seq(
        PHENOTYPE(fhir_id = "CP1", cqdg_participant_id = "P1"),
        PHENOTYPE(fhir_id = "CP2", cqdg_participant_id = "P2")
      ).toDF(),
      "normalized_diagnosis" -> Seq(
        DIAGNOSIS_INPUT(fhir_id = "D1", `subject` = "P1"),
        DIAGNOSIS_INPUT(fhir_id = "D2", `subject` = "P2"),
        DIAGNOSIS_INPUT(fhir_id = "D3", `subject` = "P1"),
      ).toDF(),
      "normalized_group" -> Seq.empty[GROUP].toDF(),
      "normalized_cause_of_death" -> Seq.empty[CAUSE_OF_DEATH].toDF(),
      "normalized_disease_status" -> Seq.empty[DISEASE_STATUS].toDF(),
      "es_index_study_centric" -> Seq(STUDY_CENTRIC()).toDF(),
      "hpo_terms" -> read(getClass.getResource("/hpo_terms.json").toString, "Json", Map(), None, None),
      "mondo_terms" -> read(getClass.getResource("/mondo_terms.json").toString, "Json", Map(), None, None),
      "icd_terms" -> read(getClass.getResource("/icd_terms.json").toString, "Json", Map(), None, None)
    )

    val output = new SimpleParticipant("re_000001", List("SD_Z6MWD3H0"))(conf).transform(data)

    output.keys should contain("simple_participant")

    val simple_participant = output("simple_participant").as[SIMPLE_PARTICIPANT].collect()

    simple_participant.length shouldBe 3

  }

}

