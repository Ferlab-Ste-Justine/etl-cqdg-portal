import bio.ferlab.datalake.commons.config.{Configuration, ConfigurationLoader, SimpleConfiguration}
import bio.ferlab.fhir.etl.centricTypes.ParticipantCentric
import model._
import org.apache.spark.sql.DataFrame
import org.scalatest.matchers.should.Matchers
import org.scalatest.flatspec.AnyFlatSpec
import pureconfig.generic.auto._

class ParticipantCentricSpec extends AnyFlatSpec with Matchers with WithSparkSession {

  import spark.implicits._

  implicit val conf: Configuration = ConfigurationLoader.loadFromResources[SimpleConfiguration]("config/dev-cqdg.conf")

  "transform" should "prepare index participant_centric" in {
    val data: Map[String, DataFrame] = Map(
      "simple_participant" -> Seq (
        SIMPLE_PARTICIPANT(`participant_id` = "P1"),
        SIMPLE_PARTICIPANT(`participant_id` = "P2")
      ).toDF(),
      "normalized_document_reference" -> Seq(
        DOCUMENTREFERENCE(`fhir_id` = "F1", `participant_id` = "P1"),
        DOCUMENTREFERENCE(`fhir_id` = "F2", `participant_id` = "P2"),
      ).toDF(),
      "normalized_biospecimen" -> Seq(
        BIOSPECIMEN_INPUT(`fhir_id` = "B1", `subject` = "P1"),
        BIOSPECIMEN_INPUT(`fhir_id` = "B2", `subject` = "P2"),
      ).toDF(),
      "normalized_task" -> Seq(
        TASK(fhir_id = "1"),
        TASK(fhir_id = "2")
      ).toDF(),
      "normalized_sample_registration" -> Seq(
        SAMPLE_INPUT(fhir_id = "S1", `subject` = "P1", `parent` = "B1"),
        SAMPLE_INPUT(fhir_id = "S2", `subject` = "P2", `parent` = "B2")
      ).toDF(),
      "es_index_study_centric" -> Seq(STUDY_CENTRIC()).toDF(),
    )

    val output = new ParticipantCentric("re_000001", List("SD_Z6MWD3H0"))(conf).transform(data)

    output.keys should contain("es_index_participant_centric")

    val participant_centric = output("es_index_participant_centric").as[PARTICIPANT_CENTRIC].collect()

    participant_centric.find(_.`participant_id` == "P1") shouldBe Some(
      PARTICIPANT_CENTRIC(
        `participant_id`= "P1",
        `vital_status` = Some("Unknown"),
        `biospecimens` = Seq(BIOSPECIMEN(`biospecimen_id` = "B1", `age_biospecimen_collection` =  17174, `sample_id` = "S1")),
        `files` = Seq(
          FILE_WITH_BIOSPECIMEN(
            `file_id` = Some("F1"),
            `file_name` = Some("file5.json"),
            `file_format` = Some("TGZ"),
            `file_size` = Some(56.0),
            `ferload_url` = Some("http://flerloadurl/outputPrefix/bc3aaa2a-63e4-4201-aec9-6b7b41a1e64a"),
            `biospecimen_reference` = Some("SAM0000001"),
            `data_type` = Some("SSUP"),
            `biospecimens` = Seq(
              BIOSPECIMEN(`biospecimen_id` = "B1", `age_biospecimen_collection` =  17174, `sample_id` = "S1")),
          ),
        )
      )
    )
  }
}
