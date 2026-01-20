import bio.ferlab.datalake.commons.config.{Configuration, ConfigurationLoader, SimpleConfiguration}
import bio.ferlab.datalake.spark3.loader.GenericLoader.read
import bio.ferlab.fhir.etl.centricTypes.ParticipantCentric
import model._
import model.input.{BIOSPECIMEN_INPUT, CODE_SYSTEM_DISPLAY_INPUT, CODE_SYSTEM_INPUT_TEXT, SAMPLE_INPUT}
import org.apache.spark.sql.{DataFrame, SaveMode}
import org.apache.spark.sql.functions.col
import org.scalatest.matchers.should.Matchers
import org.scalatest.flatspec.AnyFlatSpec
import pureconfig.generic.auto._

class ParticipantCentricSpec extends AnyFlatSpec with Matchers with WithSparkSession {

  import spark.implicits._

  implicit val conf: Configuration = ConfigurationLoader.loadFromResources[SimpleConfiguration]("config/dev-cqdg.conf")

  "transform" should "prepare index participant_centric" in {
    val data: Map[String, DataFrame] = Map(
      "simple_participant" -> Seq(
        SIMPLE_PARTICIPANT(`participant_id` = "P1"),
        SIMPLE_PARTICIPANT(`participant_id` = "P2")
      ).toDF(),
      "normalized_document_reference" -> Seq(
        DOCUMENTREFERENCE(`fhir_id` = "1", `participant_id` = "P1"),
        DOCUMENTREFERENCE(`fhir_id` = "2", `participant_id` = "P2")
      ).toDF(),
      "normalized_biospecimen" -> Seq(
        BIOSPECIMEN_INPUT(
          `fhir_id` = "B1",
          `subject` = "P1",
          `cancer_anatomic_location` = CODE_SYSTEM_INPUT_TEXT(`code` = "NCIT:C12434", `text` = Some("locationB1")),
          `tumor_histological_type` = CODE_SYSTEM_INPUT_TEXT(
            `display` = Some("Missing - Not Provided"),
            `system` = "https://fhir.cqdg.ca/CodeSystem/cqdg-specimen-missing-codes",
            `code` = "Missing - Not provided",
            `text` = Some("histological_type5")
          ),
          `biospecimen_tissue_source` = CODE_SYSTEM_DISPLAY_INPUT(`code` = "NCIT:C12434"),
          `cancer_biospecimen_type` = Some(CODEABLE(`code` = "NCIT:C12434"))
        ),
        BIOSPECIMEN_INPUT(`fhir_id` = "B2", `subject` = "P2")
      ).toDF(),
      "normalized_task" -> Seq(
        TASK(fhir_id = "1"),
        TASK(fhir_id = "2", ldm_sample_id = "S16524")
      ).toDF(),
      "normalized_sample_registration" -> Seq(
        SAMPLE_INPUT(fhir_id = "S1", `subject` = "P1", `parent` = "B1"),
        SAMPLE_INPUT(fhir_id = "S2", `subject` = "P2", `parent` = "B2")
      ).toDF(),
      "es_index_study_centric" -> Seq(STUDY_CENTRIC()).toDF(),
      "ncit_terms" -> read(getClass.getResource("/ncit_terms").toString, "Parquet", Map(), None, None)
    )

    val output = new ParticipantCentric(List("SD_Z6MWD3H0"))(conf).transform(data)

    output.keys should contain("es_index_participant_centric")

    val participant_centric = output("es_index_participant_centric").as[PARTICIPANT_CENTRIC].collect()

    participant_centric.find(_.`participant_id` == "P1") shouldBe Some(
      PARTICIPANT_CENTRIC(
        `participant_id` = "P1",
        `biospecimens` = Seq(
          BIOSPECIMEN(
            `biospecimen_id` = "B1",
            `sample_id` = "S1",
            `sample_2_id` = "S1",
            cancer_anatomic_location = CODE_SYSTEM_TEXT(
              display = "Blood (NCIT:C12434)",
              code = "NCIT:C12434",
              text = Some("locationB1")
            ),
            tumor_histological_type = CODE_SYSTEM_TEXT(
              display = "Missing - Not Provided",
              code = "Missing - Not provided",
              text = Some("histological_type5")
            )
          )
        ),
        `files` = Seq(
          FILE_WITH_BIOSPECIMEN(
            `file_id` = Some("1"),
            `file_2_id` = Some("1"),
            `file_name` = Some("file5.json"),
            `file_format` = Some("TGZ"),
            `file_size` = Some(56.0),
            `ferload_url` = Some("http://flerloadurl/outputPrefix/bc3aaa2a-63e4-4201-aec9-6b7b41a1e64a"),
            `biospecimen_reference` = Seq("SAM0000001", "SAM0000002", "SAM0000003"),
            `data_type` = Some("SSUP"),
            `dataset` = Some("Dataset1"),
            `biospecimens` = Seq(
              BIOSPECIMEN(
                biospecimen_id = "B1",
                age_biospecimen_collection = "Young",
                sample_id = "S1",
                sample_2_id = "S1",
                cancer_anatomic_location = CODE_SYSTEM_TEXT(
                  display = "Blood (NCIT:C12434)",
                  code = "NCIT:C12434",
                  text = Some("locationB1")
                ),
                tumor_histological_type = CODE_SYSTEM_TEXT(
                  display = "Missing - Not Provided",
                  code = "Missing - Not provided",
                  text = Some("histological_type5")
                )
              )
            ),
            `sequencing_experiment` = Some(SEQUENCING_EXPERIMENT_SINGLE())
          )
        )
      )
    )
  }
}
