import bio.ferlab.datalake.commons.config.{Configuration, ConfigurationLoader, SimpleConfiguration}
import bio.ferlab.fhir.etl.centricTypes.FileCentric
import model._
import org.apache.spark.sql.DataFrame
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import pureconfig.generic.auto._
import pureconfig.module.enum._

class FileCentricSpec extends AnyFlatSpec with Matchers with WithSparkSession {

  import spark.implicits._

  implicit val conf: Configuration = ConfigurationLoader.loadFromResources[SimpleConfiguration]("config/dev-cqdg.conf")

  "transform" should "prepare index file_centric" in {
    val data: Map[String, DataFrame] = Map(
      "normalized_document_reference" -> Seq(
        DOCUMENTREFERENCE(`fhir_id` = "11", `participant_id` = "P1", `biospecimen_reference` = "B1", `files` = Seq(FILE())),
      ).toDF(),
      "normalized_biospecimen" -> Seq(
        BIOSPECIMEN_INPUT(`fhir_id` = "B1", `subject` = "P1"),
        BIOSPECIMEN_INPUT(`fhir_id` = "B2", `subject` = "P1"),
      ).toDF(),
      "es_index_study_centric" -> Seq(STUDY_CENTRIC()).toDF(),
      "simple_participant" -> Seq(
        SIMPLE_PARTICIPANT(
          `participant_id` = "P1",
        ),
        SIMPLE_PARTICIPANT(
          `participant_id` = "P2",
          `gender` = "female"
        )
      ).toDF(),
      "normalized_task" -> Seq(TASK(`fhir_id` = "SXP0029366", `_for` = "P1", `alir` = "11")).toDF(),
      "normalized_sample_registration" -> Seq(
        SAMPLE_INPUT(`subject` = "P1", `parent` = "B1", `fhir_id` = "sam1"),
        SAMPLE_INPUT(`subject` = "P1", `parent` = "B2", `fhir_id` = "sam2"),
      ).toDF(),
    )

    val output = new FileCentric("5", List("STU0000001"))(conf).transform(data)
    output.keys should contain("es_index_file_centric")

    val file_centric = output("es_index_file_centric").as[FILE_CENTRIC].collect()

    file_centric.find(_.file_id == "11") shouldBe Some(
        FILE_CENTRIC(
          `file_id` = "11",
          `biospecimen_reference` = "B1",
          `data_type` = "SSUP",
          `data_category` = "Genomics",
          `file_name` = "file5.json",
          `file_format` = "TGZ",
          `file_size` = 56,
          `ferload_url` = "http://flerloadurl/outputPrefix/bc3aaa2a-63e4-4201-aec9-6b7b41a1e64a",
          `biospecimens` = Set(
            BIOSPECIMEN(
              `biospecimen_id` = "B1",
              `age_biospecimen_collection` = 17174
            ),
            BIOSPECIMEN(
              `biospecimen_id` = "B2",
              `age_biospecimen_collection` = 17174,
              `sample_id` = "sam2",
            )
          ),
          `participants` = Seq(PARTICIPANT_WITH_BIOSPECIMEN(
            `participant_id` = "P1",
            `gender` = "male",
            `age_at_recruitment` = 24566,
            `biospecimens` = Set(
              BIOSPECIMEN(
                `biospecimen_id` = "B1",
                `age_biospecimen_collection` = 17174
              ),
              BIOSPECIMEN(
                `biospecimen_id` = "B2",
                `age_biospecimen_collection` = 17174,
                `sample_id` = "sam2",
              )
            )
          )),
          `sequencing_experiment` = SEQUENCING_EXPERIMENT_SINGLE(
            `experimental_strategy` = "WXS", `alir` = "11", `snv` = "2", `gcnv` = "3", `gsv` = "4", `ssup` = "5"
          )
        )
      )
  }
}
