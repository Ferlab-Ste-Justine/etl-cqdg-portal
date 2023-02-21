import bio.ferlab.datalake.commons.config.{Configuration, ConfigurationLoader, DatasetConf, SimpleConfiguration}
import bio.ferlab.fhir.etl.centricTypes.BiospecimenCentric
import model._
import org.apache.spark.sql.DataFrame
import org.scalatest.matchers.should.Matchers
import org.scalatest.flatspec.AnyFlatSpec
import pureconfig.generic.auto._
import pureconfig.module.enum._

class BiospecimenCentricSpec extends AnyFlatSpec with Matchers with WithSparkSession {

  import spark.implicits._

  implicit val conf: Configuration = ConfigurationLoader.loadFromResources[SimpleConfiguration]("config/dev-cqdg.conf")

  "transform" should "prepare index biospecimen_centric" in {
    val data: Map[String, DataFrame] = Map(
      "simple_participant" -> Seq(
        SIMPLE_PARTICIPANT(`participant_id` = "P1"), //has file
        SIMPLE_PARTICIPANT(`participant_id` = "P2", `gender` = "female"), //does not have files
      ).toDF(),
      "normalized_document_reference" -> Seq(
        DOCUMENTREFERENCE(`fhir_id` = "D1", `participant_id` = "P1", `biospecimen_reference` = "B1", `files` = Seq(FILE())),
        DOCUMENTREFERENCE(`fhir_id` = "D2", `participant_id` = "NONE", `biospecimen_reference` = "B2", `files` = Seq(FILE())),
      ).toDF(),

      "normalized_biospecimen" -> Seq(
        BIOSPECIMEN_INPUT(`fhir_id` = "B1", `subject` = "P1"),
        BIOSPECIMEN_INPUT(`fhir_id` = "B2", `subject` = "P2"),
        BIOSPECIMEN_INPUT(`fhir_id` = "B2", `subject` = "NONE"),
      ).toDF(),
      "es_index_study_centric" -> Seq(STUDY_CENTRIC()).toDF(),
      "normalized_task" -> Seq(TASK(`fhir_id` = "SXP0029366", `_for` = "P1", `alir` = "11")).toDF(),
      "normalized_sample_registration" -> Seq(
        SAMPLE_INPUT(`subject` = "P1", `parent` = "B1", `fhir_id` = "sam1"),
        SAMPLE_INPUT(`subject` = "P1", `parent` = "B2", `fhir_id` = "sam2"),
      ).toDF(),
    )

    val output = new BiospecimenCentric("5", List("STU0000001"))(conf).transform(data)

    output.keys should contain("es_index_biospecimen_centric")

    val biospecimen_centric = output("es_index_biospecimen_centric")

    val biospecimenIds = biospecimen_centric.select("biospecimen_id").as[String].collect()

    //B2 has a participant (P2) that does not have files
    biospecimenIds should not contain "B2"

    //B3 has a participant (NONE) that does exist
    biospecimenIds should not contain "B3"

    val biospecimen_centricCollect = output("es_index_biospecimen_centric").as[BIOSPECIMEN_CENTRIC].collect()

    biospecimen_centricCollect.find(_.`biospecimen_id` == "B1") shouldBe Some(
      BIOSPECIMEN_CENTRIC(
        `biospecimen_id` = "B1",
        `age_biospecimen_collection` = 17174,
        `submitter_biospecimen_id` = "cag_sp_20832",
        `participant` = SIMPLE_PARTICIPANT(
          `participant_id` = "P1"
        ),
        `files` = Seq(
          FILE_WITH_SEQ_EXPERIMENT(
            `file_id` = "D1",
            `file_name` = "file5.json",
            `file_format` = "TGZ",
            `file_size` = "56",
            `ferload_url` = "http://flerloadurl/outputPrefix/bc3aaa2a-63e4-4201-aec9-6b7b41a1e64a",
            `biospecimen_reference` = "B1",
            `sequencing_experiment` = null
          )
        ),
        `sample_id`= "sam1",
        `submitter_sample_id` = "35849414972"
      )
    )
  }
}

