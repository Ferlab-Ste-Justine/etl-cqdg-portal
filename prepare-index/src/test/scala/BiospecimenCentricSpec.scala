import bio.ferlab.datalake.commons.config.{Configuration, ConfigurationLoader, DatasetConf, SimpleConfiguration}
import bio.ferlab.datalake.spark3.loader.GenericLoader.read
import bio.ferlab.fhir.etl.centricTypes.BiospecimenCentric
import model._
import model.input.{BIOSPECIMEN_INPUT, CODE_SYSTEM_DISPLAY_INPUT, CODE_SYSTEM_INPUT_TEXT, SAMPLE_INPUT}
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
        SIMPLE_PARTICIPANT(`participant_id` = "P1", `participant_2_id` = "P1"), // has file
        SIMPLE_PARTICIPANT(
          `participant_id` = "P2",
          `participant_2_id` = "P2",
          `gender` = DEMOGRAPHICS(),
          `sex` = "Female"
        ), // does not have files
        SIMPLE_PARTICIPANT(
          `participant_id` = "P3",
          `participant_2_id` = "P3",
          `gender` = DEMOGRAPHICS(),
          `sex` = "Female"
        )
      ).toDF(),
      "normalized_document_reference" -> Seq(
        DOCUMENTREFERENCE(
          `fhir_id` = "D1",
          `participant_id` = "P1",
          `data_type` = "Aligned Reads",
          `biospecimen_reference` = Seq("sam1"),
          `files` = Seq(FILE(`file_name` = "file1.cram", `file_format` = "CRAM"))
        ),
        DOCUMENTREFERENCE(
          `fhir_id` = "D2",
          `participant_id` = "P1",
          `data_type` = "Aligned Reads",
          `biospecimen_reference` = Seq("sam1"),
          `relates_to` = Some("D1"),
          `files` = Seq(FILE(`file_name` = "file2.crai", `file_format` = "CRAI"))
        ),
        DOCUMENTREFERENCE(
          `fhir_id` = "D3",
          `participant_id` = "NONE",
          `data_type` = "Aligned Reads",
          `biospecimen_reference` = Seq("sam3"),
          `files` = Seq(FILE(`file_name` = "file3.crai", `file_format` = "CRAI"))
        ),
        DOCUMENTREFERENCE(
          `fhir_id` = "D4",
          `participant_id` = "P3",
          `biospecimen_reference` = Seq("sam2"),
          `data_type` = "SNV",
          `files` = Seq(FILE(`file_name` = "file4.gvcf", `file_format` = "gVCF"))
        ),
        DOCUMENTREFERENCE(
          `fhir_id` = "D5",
          `participant_id` = "P3",
          `biospecimen_reference` = Seq("sam2"),
          `relates_to` = Some("D4"),
          `data_type` = "SNV",
          `files` = Seq(FILE(`file_name` = "file5.tbi", `file_format` = "TBI"))
        )
      ).toDF(),
      "normalized_biospecimen" -> Seq(
        BIOSPECIMEN_INPUT(
          `fhir_id` = "B1",
          `subject` = "P1",
          `biospecimen_tissue_source` = CODE_SYSTEM_DISPLAY_INPUT(`code` = "NCIT:C12434"),
          `cancer_anatomic_location` = CODE_SYSTEM_INPUT_TEXT(`code` = "NCIT:C12434", `text` = Some("location1")),
          `tumor_histological_type` =
            CODE_SYSTEM_INPUT_TEXT(`code` = "NCIT:C12434", `text` = Some("histological_type5")),
          `cancer_biospecimen_type` = Some(CODEABLE(`code` = "NCIT:C164014", `display` = null))
        ),
        BIOSPECIMEN_INPUT(`fhir_id` = "B2", `subject` = "P2"),
        BIOSPECIMEN_INPUT(`fhir_id` = "B3", `subject` = "NONE"),
        BIOSPECIMEN_INPUT(
          `fhir_id` = "B4",
          `subject` = "P3",
          `biospecimen_tissue_source` =
            CODE_SYSTEM_DISPLAY_INPUT(`code` = "Missing - Not collected", `display` = Some("Missing - Not Collected")),
          `cancer_anatomic_location` = CODE_SYSTEM_INPUT_TEXT(
            display = Some("Missing - Restricted Access"),
            `system` = "https://fhir.cqdg.ca/CodeSystem/cqdg-specimen-missing-codes",
            `code` = "Missing - Restricted access",
            `text` = Some("location2")
          ),
          `tumor_histological_type` =
            CODE_SYSTEM_INPUT_TEXT(`code` = "Missing - Not provided", `text` = Some("histological_type4")),
          `cancer_biospecimen_type` = Some(CODEABLE(`code` = "NCIT:XXXXX", `display` = null)),
          `tumor_normal_designation` = "Normal"
        )
      ).toDF(),
      "es_index_study_centric" -> Seq(STUDY_CENTRIC()).toDF(),
      "normalized_task" -> Seq(
        TASK(
          `fhir_id` = "SXP0029366",
          `_for` = "P1",
          `analysis_files` = Seq(
            ANALYSIS_FILE("Annotated-SNV", "FIL0000212"),
            ANALYSIS_FILE("Aligned-Reads", "D1"),
            ANALYSIS_FILE("SNV", "D4"),
            ANALYSIS_FILE("CNV", "FIL0000228"),
            ANALYSIS_FILE("SV", "FIL0000223"),
            ANALYSIS_FILE("Sequencing-Data-Supplement", "FIL0000222")
          )
        )
      ).toDF(),
      "normalized_sample_registration" -> Seq(
        SAMPLE_INPUT(`subject` = "P1", `parent` = "B1", `fhir_id` = "sam1"),
        SAMPLE_INPUT(`subject` = "P3", `parent` = "B4", `fhir_id` = "sam2")
      ).toDF(),
      "ncit_terms" -> read(getClass.getResource("/ncit_terms").toString, "Parquet", Map(), None, None)
    )

    val output = new BiospecimenCentric(List("STU0000001"))(conf).transform(data)

    output.keys should contain("es_index_biospecimen_centric")

    val biospecimen_centric = output("es_index_biospecimen_centric")

    val biospecimenIds = biospecimen_centric.select("biospecimen_id").as[String].collect()

    // B2 has a participant (P2) that does not have files
    biospecimenIds should not contain "B2"

    // B3 has a participant (NONE) that does exist
    biospecimenIds should not contain "B3"

    val biospecimen_centricCollect = output("es_index_biospecimen_centric").as[BIOSPECIMEN_CENTRIC].collect()

    biospecimen_centricCollect.find(_.`biospecimen_id` == "B1") shouldBe Some(
      BIOSPECIMEN_CENTRIC(
        `biospecimen_id` = "B1",
        `submitter_biospecimen_id` = "cag_sp_20832",
        `participant` = SIMPLE_PARTICIPANT(
          `participant_id` = "P1",
          `participant_2_id` = "P1"
        ),
        `cancer_anatomic_location` =
          CODE_SYSTEM_TEXT(display = "Blood (NCIT:C12434)", `code` = "NCIT:C12434", `text` = Some("location1")),
        `tumor_histological_type` = CODE_SYSTEM_TEXT(
          display = "Blood (NCIT:C12434)",
          `code` = "NCIT:C12434",
          `text` = Some("histological_type5")
        ),
        `cancer_biospecimen_type` = Some("Solid Tissue Specimen (NCIT:C164014)"),
        `files` = Seq(
          FILE_WITH_SEQ_EXPERIMENT(
            `file_id` = "D1",
            `file_2_id` = "D1",
            `file_name` = "file1.cram",
            `file_format` = "CRAM",
            `file_size` = "56",
            `file_hash` = "d41d8cd98f00b204e9800998ecf8427e",
//            `relates_to` = Some("D2"),  //CRAI
            `ferload_url` = "http://flerloadurl/outputPrefix/bc3aaa2a-63e4-4201-aec9-6b7b41a1e64a",
            `biospecimen_reference` = Seq("sam1"),
            `sequencing_experiment` = SEQUENCING_EXPERIMENT_SINGLE(
              `analysis_files` = Seq(
                ANALYSIS_FILE("Annotated-SNV", "FIL0000212"),
                ANALYSIS_FILE("Aligned-Reads", "D1"),
                ANALYSIS_FILE("SNV", "D4"),
                ANALYSIS_FILE("CNV", "FIL0000228"),
                ANALYSIS_FILE("SV", "FIL0000223"),
                ANALYSIS_FILE("Sequencing-Data-Supplement", "FIL0000222")
              )
            ),
            `sample` = Some(TASK_SAMPLE())
          )
        ),
        `sample_id` = "sam1",
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
          `gender` = DEMOGRAPHICS(),
          `sex` = "Female"
        ),
        `biospecimen_tissue_source` = "Missing - Not Collected",
        `cancer_anatomic_location` = CODE_SYSTEM_TEXT(
          display = "Missing - Restricted Access",
          `code` = "Missing - Restricted access",
          `text` = Some("location2")
        ),
        `tumor_histological_type` = CODE_SYSTEM_TEXT(
          display = "Missing - Not provided",
          `code` = "Missing - Not provided",
          `text` = Some("histological_type4")
        ),
        `tumor_normal_designation` = "Normal",
        `cancer_biospecimen_type` = Some("NCIT:XXXXX"),
        `files` = Seq(
          FILE_WITH_SEQ_EXPERIMENT(
            `file_id` = "D4",
            `file_2_id` = "D4",
            `file_name` = "file4.gvcf",
            `file_format` = "gVCF",
            `file_size` = "56",
            `file_hash` = "d41d8cd98f00b204e9800998ecf8427e",
//            `relates_to` = Some("D5"), //CRAI
            `ferload_url` = "http://flerloadurl/outputPrefix/bc3aaa2a-63e4-4201-aec9-6b7b41a1e64a",
            `biospecimen_reference` = Seq("sam2"),
            `sequencing_experiment` = SEQUENCING_EXPERIMENT_SINGLE(
              `analysis_files` = Seq(
                ANALYSIS_FILE("Annotated-SNV", "FIL0000212"),
                ANALYSIS_FILE("Aligned-Reads", "D1"),
                ANALYSIS_FILE("SNV", "D4"),
                ANALYSIS_FILE("CNV", "FIL0000228"),
                ANALYSIS_FILE("SV", "FIL0000223"),
                ANALYSIS_FILE("Sequencing-Data-Supplement", "FIL0000222")
              )
            ),
            `sample` = Some(TASK_SAMPLE())
          )
        ),
        `sample_id` = "sam2",
        `submitter_sample_id` = "35849414972"
      )
    )
  }

  it should "link each sample only to its own files and keep samples without files" in {
    val data: Map[String, DataFrame] = Map(
      "simple_participant" -> Seq(
        SIMPLE_PARTICIPANT(`participant_id` = "P1", `participant_2_id` = "P1"),
        SIMPLE_PARTICIPANT(`participant_id` = "P2", `participant_2_id` = "P2"),
        SIMPLE_PARTICIPANT(`participant_id` = "P3", `participant_2_id` = "P3") // no files
      ).toDF(),
      "normalized_document_reference" -> Seq(
        DOCUMENTREFERENCE(`fhir_id` = "D1", `participant_id` = "P1", `biospecimen_reference` = Seq("sam1")),
        DOCUMENTREFERENCE(`fhir_id` = "D2", `participant_id` = "P1", `biospecimen_reference` = Seq("sam2")),
        // multi-sample file: one FH, one FI per sample
        DOCUMENTREFERENCE(
          `fhir_id` = "D3",
          `participant_id` = "P1",
          `biospecimen_reference` = Seq("sam1"),
          `files` = Seq(FILE(`stable_file_id` = "FH9"))
        ),
        DOCUMENTREFERENCE(
          `fhir_id` = "D4",
          `participant_id` = "P2",
          `biospecimen_reference` = Seq("sam4"),
          `files` = Seq(FILE(`stable_file_id` = "FH9"))
        )
      ).toDF(),
      "normalized_biospecimen" -> Seq(
        BIOSPECIMEN_INPUT(`fhir_id` = "B1", `subject` = "P1"),
        BIOSPECIMEN_INPUT(`fhir_id` = "B2", `subject` = "P1"),
        BIOSPECIMEN_INPUT(`fhir_id` = "B3", `subject` = "P1"),
        BIOSPECIMEN_INPUT(`fhir_id` = "B4", `subject` = "P2"),
        BIOSPECIMEN_INPUT(`fhir_id` = "B5", `subject` = "P3")
      ).toDF(),
      "es_index_study_centric" -> Seq(STUDY_CENTRIC()).toDF(),
      "normalized_task" -> Seq.empty[TASK].toDF(),
      "normalized_sample_registration" -> Seq(
        SAMPLE_INPUT(`subject` = "P1", `parent` = "B1", `fhir_id` = "sam1"),
        SAMPLE_INPUT(`subject` = "P1", `parent` = "B1", `fhir_id` = "sam1b"), // no file of its own
        SAMPLE_INPUT(`subject` = "P1", `parent` = "B2", `fhir_id` = "sam2"),
        SAMPLE_INPUT(`subject` = "P1", `parent` = "B3", `fhir_id` = "sam3"), // no file of its own
        SAMPLE_INPUT(`subject` = "P2", `parent` = "B4", `fhir_id` = "sam4"),
        SAMPLE_INPUT(`subject` = "P3", `parent` = "B5", `fhir_id` = "sam5")
      ).toDF(),
      "ncit_terms" -> read(getClass.getResource("/ncit_terms").toString, "Parquet", Map(), None, None)
    )

    val output = new BiospecimenCentric(List("STU0000001"))(conf).transform(data)

    val filesPerSample: Map[(String, String), Seq[String]] = output("es_index_biospecimen_centric")
      .select("biospecimen_id", "sample_id", "files.file_id")
      .as[(String, String, Seq[String])]
      .collect()
      .map { case (biospecimenId, sampleId, fileIds) => (biospecimenId, sampleId) -> fileIds }
      .toMap

    // samples of participants without files (P3) are still not indexed
    filesPerSample.keySet shouldBe Set(("B1", "sam1"), ("B1", "sam1b"), ("B2", "sam2"), ("B3", "sam3"), ("B4", "sam4"))

    // participant with several samples: each sample only has its own files, samples without files are kept
    filesPerSample(("B1", "sam1")) should contain theSameElementsAs Seq("D1", "D3")
    filesPerSample(("B1", "sam1b")) shouldBe empty
    filesPerSample(("B2", "sam2")) should contain theSameElementsAs Seq("D2")
    filesPerSample(("B3", "sam3")) shouldBe empty

    // multi-sample FH (D3 and D4): each sample keeps its own FI
    filesPerSample(("B4", "sam4")) should contain theSameElementsAs Seq("D4")

    // filtered on a given file, only its own sample comes up
    filesPerSample.toSeq.collect { case (sample, files) if files.contains("D2") => sample } shouldBe Seq(("B2", "sam2"))
  }
}
