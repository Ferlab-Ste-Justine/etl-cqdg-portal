import bio.ferlab.datalake.commons.config.{Configuration, ConfigurationLoader, DatasetConf, SimpleConfiguration}
import bio.ferlab.datalake.spark3.loader.GenericLoader.read
import bio.ferlab.fhir.etl.centricTypes.BiospecimenCentric
import model._
import org.apache.spark.sql.DataFrame
import org.scalatest.matchers.should.Matchers
import org.scalatest.flatspec.AnyFlatSpec
import pureconfig.generic.auto._

class BiospecimenCentricSpec extends AnyFlatSpec with Matchers with WithSparkSession {

  import spark.implicits._

  implicit val conf: Configuration = ConfigurationLoader.loadFromResources[SimpleConfiguration]("config/dev-cqdg.conf")

  "transform" should "prepare index biospecimen_centric" in {
    val data: Map[String, DataFrame] = Map(
      "simple_participant" -> Seq(
        SIMPLE_PARTICIPANT(`participant_id` = "P1", `participant_2_id` = "P1"), //has file
        SIMPLE_PARTICIPANT(`participant_id` = "P2", `participant_2_id` = "P2", `sex` = "female"), //does not have files
        SIMPLE_PARTICIPANT(`participant_id` = "P3", `participant_2_id` = "P3", `sex` = "female"),
      ).toDF(),
      "normalized_document_reference" -> Seq(
        DOCUMENTREFERENCE(
          `fhir_id` = "D1",
          `participant_id` = "P1",
          `data_type` = "Aligned Reads",
          `biospecimen_reference` = Seq("B1"),
          `files` = Seq(FILE(`file_name` = "file1.cram", `file_format` = "CRAM"))
        ),
        DOCUMENTREFERENCE(
          `fhir_id` = "D2",
          `participant_id` = "P1",
          `data_type` = "Aligned Reads",
          `biospecimen_reference` = Seq("B1"),
          `relates_to` = Some("D1"),
          `files` = Seq(FILE(`file_name` = "file2.crai", `file_format` = "CRAI"))
        ),
        DOCUMENTREFERENCE(
          `fhir_id` = "D3",
          `participant_id` = "NONE",
          `data_type` = "Aligned Reads",
          `biospecimen_reference` = Seq("B3"),
          `files` = Seq(FILE(`file_name` = "file3.crai", `file_format` = "CRAI"))
        ),
        DOCUMENTREFERENCE(
          `fhir_id` = "D4",
          `participant_id` = "P3",
          `biospecimen_reference` = Seq("B4"),
          `data_type` = "SNV",
          `files` = Seq(FILE(`file_name` = "file4.gvcf", `file_format` = "gVCF"))
        ),
        DOCUMENTREFERENCE(
          `fhir_id` = "D5",
          `participant_id` = "P3",
          `biospecimen_reference` = Seq("B4"),
          `relates_to` = Some("D4"),
          `data_type` = "SNV",
          `files` = Seq(FILE(`file_name` = "file5.tbi", `file_format` = "TBI"))
        ),
      ).toDF(),

      "normalized_biospecimen" -> Seq(
        BIOSPECIMEN_INPUT(`fhir_id` = "B1", `subject` = "P1"),
        BIOSPECIMEN_INPUT(`fhir_id` = "B2", `subject` = "P2"),
        BIOSPECIMEN_INPUT(`fhir_id` = "B3", `subject` = "NONE"),
        BIOSPECIMEN_INPUT(`fhir_id` = "B4", `subject` = "P3"),
      ).toDF(),
      "es_index_study_centric" -> Seq(STUDY_CENTRIC()).toDF(),
      "normalized_task" -> Seq(TASK(`fhir_id` = "SXP0029366", `_for` = "P1", `analysis_files` = Seq(ANALYSIS_FILE("Annotated-SNV", "FIL0000212"),
        ANALYSIS_FILE("Aligned-reads", "FIL0000227"),
        ANALYSIS_FILE("SNV", "FIL0000225"),
        ANALYSIS_FILE("Germline-CNV", "FIL0000228"),
        ANALYSIS_FILE("Germline-structural-variant", "FIL0000223"),
        ANALYSIS_FILE("Sequencing-data-supplement", "FIL0000222")))).toDF(),
      "normalized_sample_registration" -> Seq(
        SAMPLE_INPUT(`subject` = "P1", `parent` = "B1", `fhir_id` = "sam1"),
        SAMPLE_INPUT(`subject` = "P3", `parent` = "B4", `fhir_id` = "sam2"),
      ).toDF(),
      "ncit_terms" -> read(getClass.getResource("/ncit_terms").toString, "Parquet", Map(), None, None),
    )

    val output = new BiospecimenCentric(List("STU0000001"))(conf).transform(data)

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
        `submitter_biospecimen_id` = "cag_sp_20832",
        `participant` = SIMPLE_PARTICIPANT(
          `participant_id` = "P1",
          `participant_2_id` = "P1",
        ),
        `files` = Seq(
          FILE_WITH_SEQ_EXPERIMENT(
            `file_id` = "D1",
            `file_2_id` = "D1",
            `file_name` = "file1.cram",
            `file_format` = "CRAM",
            `file_size` = "56",
//            `relates_to` = Some("D2"),  //CRAI
            `ferload_url` = "http://flerloadurl/outputPrefix/bc3aaa2a-63e4-4201-aec9-6b7b41a1e64a",
            `biospecimen_reference` = Seq("B1"),
            `sequencing_experiment` = null
          )
        ),
        `sample_id`= "sam1",
        `submitter_sample_id` = "35849414972"
      )
    )

    biospecimen_centricCollect.find(_.`biospecimen_id` == "B4") shouldBe Some(
      BIOSPECIMEN_CENTRIC(
        `biospecimen_id` = "B4",
        `submitter_biospecimen_id` = "cag_sp_20832",
        `participant` = SIMPLE_PARTICIPANT(
          `participant_id` = "P3",
          `participant_2_id` = "P3",
          `sex` = "female"
        ),
        `files` = Seq(
          FILE_WITH_SEQ_EXPERIMENT(
            `file_id` = "D4",
            `file_2_id` = "D4",
            `file_name` = "file4.gvcf",
            `file_format` = "gVCF",
            `file_size` = "56",
//            `relates_to` = Some("D5"), //CRAI
            `ferload_url` = "http://flerloadurl/outputPrefix/bc3aaa2a-63e4-4201-aec9-6b7b41a1e64a",
            `biospecimen_reference` = Seq("B4"),
            `sequencing_experiment` = null
          )
        ),
        `sample_id` = "sam2",
        `submitter_sample_id` = "35849414972"
      )
    )
  }
}

