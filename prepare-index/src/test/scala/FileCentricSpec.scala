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
        DOCUMENTREFERENCE(
          `fhir_id` = "11",
          `participant_id` = "P1",
          `biospecimen_reference` = Seq("sam1"),
          `files` = Seq(FILE())
        ),
        DOCUMENTREFERENCE(
          `fhir_id` = "12",
          `participant_id` = "P1",
          `biospecimen_reference` = Seq("sam2"),
          `data_type` = "Aligned Reads",
          `files` = Seq(FILE(`file_name` = "file.1", `file_format` = "CRAM"))
        ),
        DOCUMENTREFERENCE(
          `fhir_id` = "13",
          `participant_id` = "P1",
          `biospecimen_reference` = Seq("sam2"),
          `relates_to` = Some("12"),
          `files` = Seq(FILE(`file_name` = "file.2", `file_format` = "CRAI"))
        )
      ).toDF(),
      "normalized_biospecimen" -> Seq(
        BIOSPECIMEN_INPUT(
          `fhir_id` = "B1",
          `subject` = "P1",
          `cancer_biospecimen_type` = Some(CODEABLE(`code` = "NCIT:C12434"))
        ),
        BIOSPECIMEN_INPUT(
          `fhir_id` = "B2",
          `subject` = "P1",
          `cancer_biospecimen_type` = None
        )
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
          `gender` = DEMOGRAPHICS(),
          `sex` = "Female",
          `family_relationships` = Seq(
            FAMILY_RELATIONSHIP_WITH_FAMILY(),
            FAMILY_RELATIONSHIP_WITH_FAMILY(
              `participant_id` = "P2",
              `submitter_participant_id` = "EXT_P2",
              `focus_participant_id` = "P1",
              `relationship_to_proband` = "Proband",
              `family_id` = "FAM1"
            )
          )
        )
      ).toDF(),
      "normalized_task" -> Seq(
        TASK(
          `fhir_id` = "SXP0029366",
          `_for` = "P1",
          `analysis_files` = Seq(
            ANALYSIS_FILE("Aligned-Reads", "12"),
            ANALYSIS_FILE("Sequencing-Data-Supplement", "11")
          )
        )
      ).toDF(),
      "normalized_sample_registration" -> Seq(
        SAMPLE_INPUT(`subject` = "P1", `parent` = "B1", `fhir_id` = "sam1"),
        SAMPLE_INPUT(`subject` = "P1", `parent` = "B2", `fhir_id` = "sam2")
      ).toDF(),
      "ncit_terms" -> GenericLoader.read(getClass.getResource("/ncit_terms").toString, "Parquet", Map(), None, None)
    )

    val output = new FileCentric(List("STU0000001"))(conf).transform(data)
    output.keys should contain("es_index_file_centric")

    val file_centric = output("es_index_file_centric").as[FILE_CENTRIC].collect()

    output("es_index_file_centric").count() shouldEqual 2 // CRAI files are excluded

    val biospecimenB1 = BIOSPECIMEN(
      `biospecimen_id` = "B1",
      cancer_anatomic_location = CODE_SYSTEM_TEXT(
        display = "Blood (NCIT:C12434)",
        code = "NCIT:C12434",
        text = Some("location")
      ),
      tumor_histological_type = CODE_SYSTEM_TEXT(
        display = "Missing - Not Provided",
        code = "Missing - Not provided",
        text = Some("histological_type5")
      )
    )

    val biospecimenB2 = BIOSPECIMEN(
      `biospecimen_id` = "B2",
      `sample_id` = "sam2",
      `sample_2_id` = "sam2",
      cancer_anatomic_location = CODE_SYSTEM_TEXT(
        display = "Blood (NCIT:C12434)",
        code = "NCIT:C12434",
        text = Some("location")
      ),
      tumor_histological_type = CODE_SYSTEM_TEXT(
        display = "Missing - Not Provided",
        code = "Missing - Not provided",
        text = Some("histological_type5")
      ),
      `cancer_biospecimen_type` = None
    )

    def participantP1(biospecimens: Set[BIOSPECIMEN]) = PARTICIPANT_WITH_BIOSPECIMEN(
      `participant_id` = "P1",
      `participant_2_id` = "P1",
      `gender` = DEMOGRAPHICS(`code` = "Man"),
      `sex` = "Male",
      `sex_at_birth` = DEMOGRAPHICS(
        `code` = "Male",
        `collect_method` = CODEABLE(`code` = "Clinician-recorded", `display` = null)
      ),
      `biospecimens` = biospecimens
    )

    // P1 has 2 samples (sam1 on B1, sam2 on B2): each file only carries the sample of its biospecimen_reference
    file_centric.find(_.file_id == "11") shouldBe Some(
      FILE_CENTRIC(
        `file_id` = "11",
        `biospecimen_reference` = Seq("sam1"),
        `data_type` = "SSUP",
        `data_category` = "Genomics",
        `file_name` = "file5.json",
        `file_format` = "TGZ",
        `file_size` = 56,
        `file_hash` = "d41d8cd98f00b204e9800998ecf8427e",
        `ferload_url` = "http://flerloadurl/outputPrefix/bc3aaa2a-63e4-4201-aec9-6b7b41a1e64a",
        `biospecimens` = Set(biospecimenB1),
        `participants` = Seq(participantP1(Set(biospecimenB1))),
        `sequencing_experiment` = SEQUENCING_EXPERIMENT_SINGLE(
          `experimental_strategies_1` = Seq(CODEABLE("WXS")),
          `analysis_files` = Seq(
            ANALYSIS_FILE("Aligned-Reads", "12"),
            ANALYSIS_FILE("Sequencing-Data-Supplement", "11")
          )
        )
      )
    )

    file_centric.find(_.file_id == "12") shouldBe Some(
      FILE_CENTRIC(
        `file_id` = "12",
        `biospecimen_reference` = Seq("sam2"),
        `data_type` = "Aligned Reads",
        `data_category` = "Genomics",
        `file_name` = "file.1",
        `file_format` = "CRAM",
        `file_size` = 56,
        `relates_to` = Some(RELATES_TO()),
        `ferload_url` = "http://flerloadurl/outputPrefix/bc3aaa2a-63e4-4201-aec9-6b7b41a1e64a",
        `biospecimens` = Set(biospecimenB2),
        `participants` = Seq(participantP1(Set(biospecimenB2))),
        `sequencing_experiment` = SEQUENCING_EXPERIMENT_SINGLE(
          `experimental_strategies_1` = Seq(CODEABLE("WXS")),
          `analysis_files` = Seq(
            ANALYSIS_FILE("Aligned-Reads", "12"),
            ANALYSIS_FILE("Sequencing-Data-Supplement", "11")
          )
        )
      )
    )
  }

  it should "link each file only to the samples of its biospecimen_reference" in {
    val data: Map[String, DataFrame] = Map(
      "normalized_document_reference" -> Seq(
        // multi-sample file: one FH, one FI per sample (P1/sam1 and P2/sam3)
        DOCUMENTREFERENCE(
          `fhir_id` = "21",
          `participant_id` = "P1",
          `biospecimen_reference` = Seq("sam1"),
          `files` = Seq(FILE(`stable_file_id` = "FH0000201"))
        ),
        DOCUMENTREFERENCE(
          `fhir_id` = "22",
          `participant_id` = "P2",
          `biospecimen_reference` = Seq("sam3"),
          `files` = Seq(FILE(`stable_file_id` = "FH0000201"))
        ),
        // P1 has a second sample with its own file
        DOCUMENTREFERENCE(
          `fhir_id` = "23",
          `participant_id` = "P1",
          `biospecimen_reference` = Seq("sam2"),
          `files` = Seq(FILE(`stable_file_id` = "FH0000202"))
        )
      ).toDF(),
      "normalized_biospecimen" -> Seq(
        BIOSPECIMEN_INPUT(`fhir_id` = "B1", `subject` = "P1"),
        BIOSPECIMEN_INPUT(`fhir_id` = "B2", `subject` = "P1"),
        BIOSPECIMEN_INPUT(`fhir_id` = "B3", `subject` = "P2")
      ).toDF(),
      "es_index_study_centric" -> Seq(STUDY_CENTRIC()).toDF(),
      "simple_participant" -> Seq(
        SIMPLE_PARTICIPANT(`participant_id` = "P1", `participant_2_id` = "P1"),
        SIMPLE_PARTICIPANT(`participant_id` = "P2", `participant_2_id` = "P2")
      ).toDF(),
      "normalized_task" -> Seq.empty[TASK].toDF(),
      "normalized_sample_registration" -> Seq(
        SAMPLE_INPUT(`subject` = "P1", `parent` = "B1", `fhir_id` = "sam1"),
        SAMPLE_INPUT(`subject` = "P1", `parent` = "B2", `fhir_id` = "sam2"),
        SAMPLE_INPUT(`subject` = "P2", `parent` = "B3", `fhir_id` = "sam3")
      ).toDF(),
      "ncit_terms" -> GenericLoader.read(getClass.getResource("/ncit_terms").toString, "Parquet", Map(), None, None)
    )

    val output = new FileCentric(List("STU0000001"))(conf).transform(data)
    val file_centric = output("es_index_file_centric").as[FILE_CENTRIC].collect()

    def samplesOf(fileId: String): (Set[(String, String)], Set[(String, String)]) = {
      val file = file_centric.find(_.file_id == fileId).get
      (
        file.`biospecimens`.map(b => (b.`biospecimen_id`, b.`sample_id`)),
        file.`participants`.flatMap(_.`biospecimens`).map(b => (b.`biospecimen_id`, b.`sample_id`)).toSet
      )
    }

    file_centric.map(_.file_id) should contain theSameElementsAs Seq("21", "22", "23")

    // participant with 2 samples: each file keeps only its own sample
    samplesOf("21") shouldBe (Set(("B1", "sam1")), Set(("B1", "sam1")))
    samplesOf("23") shouldBe (Set(("B2", "sam2")), Set(("B2", "sam2")))

    // multi-sample FH: each FI keeps its own sample; participant with a single sample is unchanged
    samplesOf("22") shouldBe (Set(("B3", "sam3")), Set(("B3", "sam3")))
    file_centric.filter(_.stable_file_id == "FH0000201").map(_.file_id) should contain theSameElementsAs Seq("21", "22")
  }
}
