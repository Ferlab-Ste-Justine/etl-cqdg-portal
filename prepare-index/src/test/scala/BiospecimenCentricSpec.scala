import bio.ferlab.datalake.commons.config.{Configuration, ConfigurationLoader}
import bio.ferlab.fhir.etl.centricTypes.BiospecimenCentric
import model._
import org.apache.spark.sql.DataFrame
import org.scalatest.matchers.should.Matchers
import org.scalatest.flatspec.AnyFlatSpec

class BiospecimenCentricSpec extends AnyFlatSpec with Matchers with WithSparkSession {

  import spark.implicits._

  implicit val conf: Configuration = ConfigurationLoader.loadFromResources("config/dev-cqdg.conf")

  "transform" should "prepare index biospecimen_centric" in {
    val data: Map[String, DataFrame] = Map(
      "simple_participant" -> Seq(
        SIMPLE_PARTICIPANT(`participant_id` = "P1", `gender` = "mala"), //has file
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

//    biospecimen_centric.as[BIOSPECIMEN_CENTRIC].collect() should contain theSameElementsAs
//      Seq(
//        BIOSPECIMEN_CENTRIC(
//          `fhir_id` = "111",
//          biospecimen_facet_ids = BIOSPECIMEN_FACET_IDS(biospecimen_fhir_id_1 = "111", biospecimen_fhir_id_2 = "111"),
//          `participant_fhir_id` = "1",
//          `participant` = SIMPLE_PARTICIPANT(`fhir_id` = "1", participant_facet_ids = PARTICIPANT_FACET_IDS(participant_fhir_id_1 = "1", participant_fhir_id_2 = "1")),
//          `nb_files` = 3,
//          `files` = Seq(
//            DOCUMENTREFERENCE_WITH_SEQ_EXP(
//              `fhir_id` = "11",
//              file_facet_ids = FILE_FACET_IDS(file_fhir_id_1 = "11", file_fhir_id_2 = "11"),
//              `sequencing_experiment` = SEQUENCING_EXPERIMENT()
//            ),
//            DOCUMENTREFERENCE_WITH_SEQ_EXP(
//              `fhir_id` = "33",
//              file_facet_ids = FILE_FACET_IDS(file_fhir_id_1 = "33", file_fhir_id_2 = "33"),
//              `sequencing_experiment` = SEQUENCING_EXPERIMENT()
//            ),
//            DOCUMENTREFERENCE_WITH_SEQ_EXP(
//              `fhir_id` = "44",
//              file_facet_ids = FILE_FACET_IDS(file_fhir_id_1 = "44", file_fhir_id_2 = "44"),
//              `sequencing_experiment` = SEQUENCING_EXPERIMENT()
//            )
//          )
//        ),
//        BIOSPECIMEN_CENTRIC(
//          `fhir_id` = "222",
//          biospecimen_facet_ids = BIOSPECIMEN_FACET_IDS(biospecimen_fhir_id_1 = "222", biospecimen_fhir_id_2 = "222"),
//          `participant_fhir_id` = "2",
//          `participant` = SIMPLE_PARTICIPANT(`fhir_id` = "2", participant_facet_ids = PARTICIPANT_FACET_IDS(participant_fhir_id_1 = "2", participant_fhir_id_2 = "2")),
//          `nb_files` = 3,
//          `files` = Seq(
//            DOCUMENTREFERENCE_WITH_SEQ_EXP(
//              `fhir_id` = "21",
//              file_facet_ids = FILE_FACET_IDS(file_fhir_id_1 = "21", file_fhir_id_2 = "21"),
//              `sequencing_experiment` = SEQUENCING_EXPERIMENT()
//            ),
//            DOCUMENTREFERENCE_WITH_SEQ_EXP(
//              `fhir_id` = "33",
//              file_facet_ids = FILE_FACET_IDS(file_fhir_id_1 = "33", file_fhir_id_2 = "33"),
//              `sequencing_experiment` = SEQUENCING_EXPERIMENT()
//            ),
//            DOCUMENTREFERENCE_WITH_SEQ_EXP(
//              `fhir_id` = "44",
//              file_facet_ids = FILE_FACET_IDS(file_fhir_id_1 = "44", file_fhir_id_2 = "44"),
//              `sequencing_experiment` = SEQUENCING_EXPERIMENT()
//            )
//          )
//        ))
  }
}

