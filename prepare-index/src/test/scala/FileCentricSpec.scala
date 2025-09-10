import bio.ferlab.datalake.commons.config.{Configuration, ConfigurationLoader, SimpleConfiguration}
import bio.ferlab.datalake.spark3.loader.GenericLoader
import bio.ferlab.fhir.etl.centricTypes.FileCentric
import model._
import model.input.{BIOSPECIMEN_INPUT, SAMPLE_INPUT}
import org.apache.spark.sql.DataFrame
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import pureconfig.generic.auto._

class FileCentricSpec extends AnyFlatSpec with Matchers with WithSparkSession {

  import spark.implicits._

  implicit val conf: Configuration = ConfigurationLoader.loadFromResources[SimpleConfiguration]("config/dev-cqdg.conf")

  "transform" should "prepare index file_centric" in {
    val data: Map[String, DataFrame] = Map(
      "normalized_document_reference" -> Seq(
        DOCUMENTREFERENCE(`fhir_id` = "11", `participant_id` = "P1", `biospecimen_reference` = Seq("B1"), `files` = Seq(FILE())),
        DOCUMENTREFERENCE(`fhir_id` = "12", `participant_id` = "P1", `biospecimen_reference` = Seq("B2"), `data_type` = "Aligned Reads", `files` = Seq(FILE(`file_name` = "file.1", `file_format` = "CRAM"))),
        DOCUMENTREFERENCE(`fhir_id` = "13", `participant_id` = "P1", `biospecimen_reference` = Seq("B2"), `relates_to` = Some("12"), `files` = Seq(FILE(`file_name` = "file.2", `file_format` = "CRAI"))),
      ).toDF(),
      "normalized_biospecimen" -> Seq(
        BIOSPECIMEN_INPUT(`fhir_id` = "B1", `subject` = "P1"),
        BIOSPECIMEN_INPUT(`fhir_id` = "B2", `subject` = "P1"),
      ).toDF(),
      "es_index_study_centric" -> Seq(STUDY_CENTRIC()).toDF(),
      "simple_participant" -> Seq(
        SIMPLE_PARTICIPANT(
          `participant_id` = "P1",
          `participant_2_id` = "P1",
          `family_relationships` = Seq(FAMILY_RELATIONSHIP_WITH_FAMILY())
        ),
        SIMPLE_PARTICIPANT(
          `participant_id` = "P2",
          `participant_2_id` = "P2",
          `gender` = "Woman",
          `sex` = "Female",
          `family_relationships` = Seq(FAMILY_RELATIONSHIP_WITH_FAMILY(), FAMILY_RELATIONSHIP_WITH_FAMILY(`participant_id` = "P2", `submitter_participant_id` = "EXT_P2", `focus_participant_id` = "P1", `relationship_to_proband` = "Proband", `family_id` = "FAM1"))
        )
      ).toDF(),
      "normalized_task" -> Seq(TASK(`fhir_id` = "SXP0029366", `_for` = "P1",  `analysis_files` = Seq(
        ANALYSIS_FILE("Aligned-reads", "12"),
        ANALYSIS_FILE("Sequencing-data-supplement", "11"),
      ))).toDF(),
      "normalized_sample_registration" -> Seq(
        SAMPLE_INPUT(`subject` = "P1", `parent` = "B1", `fhir_id` = "sam1"),
        SAMPLE_INPUT(`subject` = "P1", `parent` = "B2", `fhir_id` = "sam2"),
      ).toDF(),
      "ncit_terms" -> GenericLoader.read(getClass.getResource("/ncit_terms").toString, "Parquet", Map(), None, None),
    )

    val output = new FileCentric(List("STU0000001"))(conf).transform(data)
    output.keys should contain("es_index_file_centric")

    val file_centric = output("es_index_file_centric").as[FILE_CENTRIC].collect()

    output("es_index_file_centric").count() shouldEqual 2 //CRAI files are excluded

    val sortedFileCentric = file_centric.map(fc =>
      fc.copy(
        biospecimens = fc.`biospecimens`.toSeq.sortBy(_.`biospecimen_id`).toSet
      )
    )

    sortedFileCentric.find(_.file_id == "11") shouldBe Some(
        FILE_CENTRIC(
          `file_id` = "11",
          `biospecimen_reference` = Seq("B1"),
          `data_type` = "SSUP",
          `data_category` = "Genomics",
          `file_name` = "file5.json",
          `file_format` = "TGZ",
          `file_size` = 56,
          `ferload_url` = "http://flerloadurl/outputPrefix/bc3aaa2a-63e4-4201-aec9-6b7b41a1e64a",
          `biospecimens` = Set(
            BIOSPECIMEN(
              `biospecimen_id` = "B2",
              `sample_id` = "sam2",
              `sample_2_id` = "sam2",
            ),
            BIOSPECIMEN(
              `biospecimen_id` = "B1",
            ),
          ),
          `participants` = Seq(PARTICIPANT_WITH_BIOSPECIMEN(
            `participant_id` = "P1",
            `participant_2_id` = "P1",
            `gender` = "Man",
            `sex` = "Male",
            `biospecimens` = Set(
              BIOSPECIMEN(
                `biospecimen_id` = "B2",
                `sample_id` = "sam2",
                `sample_2_id` = "sam2",
              ),
              BIOSPECIMEN(
                `biospecimen_id` = "B1",
              ),
            )
          )),
          `sequencing_experiment` = SEQUENCING_EXPERIMENT_SINGLE(
            `experimental_strategy_1` = CODEABLE("WXS"), `analysis_files` = Seq(
              ANALYSIS_FILE("Aligned-reads", "12"),
              ANALYSIS_FILE("Sequencing-data-supplement", "11"),
            )
          )
        )
      )

    file_centric.find(_.file_id == "12") shouldBe Some(
      FILE_CENTRIC(
        `file_id` = "12",
        `biospecimen_reference` = Seq("B2"),
        `data_type` = "Aligned Reads",
        `data_category` = "Genomics",
        `file_name` = "file.1",
        `file_format` = "CRAM",
        `file_size` = 56,
        `relates_to`= Some(RELATES_TO()),
        `ferload_url` = "http://flerloadurl/outputPrefix/bc3aaa2a-63e4-4201-aec9-6b7b41a1e64a",
        `biospecimens` = Set(
          BIOSPECIMEN(
            `biospecimen_id` = "B2",
            `sample_id` = "sam2",
            `sample_2_id` = "sam2",
          ),
          BIOSPECIMEN(
            `biospecimen_id` = "B1",
          ),
        ),
        `participants` = Seq(PARTICIPANT_WITH_BIOSPECIMEN(
          `participant_id` = "P1",
          `participant_2_id` = "P1",
          `gender` = "Man",
          `sex` = "Male",
          `biospecimens` = Set(
            BIOSPECIMEN(
              `biospecimen_id` = "B2",
              `sample_id` = "sam2",
              `sample_2_id` = "sam2",
            ),
            BIOSPECIMEN(
              `biospecimen_id` = "B1",
            ),
          )
        )),
        `sequencing_experiment` = SEQUENCING_EXPERIMENT_SINGLE(
          `experimental_strategy_1` = CODEABLE("WXS"), `analysis_files` = Seq(
            ANALYSIS_FILE("Aligned-reads", "12"),
            ANALYSIS_FILE("Sequencing-data-supplement", "11"),
          )
        )
      )
    )
  }
}
