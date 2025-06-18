import bio.ferlab.datalake.commons.config.{Configuration, ConfigurationLoader, SimpleConfiguration}
import bio.ferlab.datalake.spark3.loader.GenericLoader.read
import bio.ferlab.fhir.etl.centricTypes.{ProgramCentric, StudyCentric}
import model._
import model.centric.{PROGRAM_CENTRIC, CONTACT => PROGRAM_CONTACT}
import model.input.LIST_INPUT
import org.apache.spark.sql.DataFrame
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ProgramCentricSpec extends AnyFlatSpec with Matchers with WithSparkSession {

  import spark.implicits._

  implicit val conf: Configuration = ConfigurationLoader.loadFromResources[SimpleConfiguration]("config/dev-cqdg.conf")

  val patient1: PATIENT_INPUT =  PATIENT_INPUT(`fhir_id` = "PRT0000001")
  val patient2: PATIENT_INPUT =  PATIENT_INPUT( `fhir_id` = "PRT0000002", `age_at_recruitment` = "Young", `submitter_participant_id` = "35849419216")
  val patient3: PATIENT_INPUT =  PATIENT_INPUT( `fhir_id` = "PRT0000003", `age_at_recruitment` = "Young", `ethnicity` = "aboriginal" ,`submitter_participant_id` = "35849430470")
  val patient4: PATIENT_INPUT =  PATIENT_INPUT(`fhir_id` = "PRT0000004", `submitter_participant_id` = "Excluded1")
  val patient5: PATIENT_INPUT =  PATIENT_INPUT( `fhir_id` = "PRT0000005", `submitter_participant_id` = "Excluded2")
  val patient6: PATIENT_INPUT =  PATIENT_INPUT( `fhir_id` = "PRT0000006", `submitter_participant_id` = "Excluded3")

  val family1: FAMILY_RELATIONSHIP_NEW = FAMILY_RELATIONSHIP_NEW()
  val family2: FAMILY_RELATIONSHIP_NEW = FAMILY_RELATIONSHIP_NEW(`internal_family_relationship_id` = "FAM0000002FR", `submitter_participant_id` = "PRT0000002", `relationship_to_proband` = "Father")
  val family3: FAMILY_RELATIONSHIP_NEW = FAMILY_RELATIONSHIP_NEW(`internal_family_relationship_id` = "FAM0000003FR", `submitter_participant_id` = "PRT0000003", `relationship_to_proband` = "Proband")
  val family4: FAMILY_RELATIONSHIP_NEW = FAMILY_RELATIONSHIP_NEW(`internal_family_relationship_id` = "Excluded1", `submitter_participant_id` = "PRT0000004", `relationship_to_proband` = "Mother")
  val family5: FAMILY_RELATIONSHIP_NEW = FAMILY_RELATIONSHIP_NEW(`internal_family_relationship_id` = "Excluded2", `submitter_participant_id` = "PRT0000005", `relationship_to_proband` = "Father")
  val family6: FAMILY_RELATIONSHIP_NEW = FAMILY_RELATIONSHIP_NEW(`internal_family_relationship_id` = "Excluded3", `submitter_participant_id` = "PRT0000006", `relationship_to_proband` = "Proband")

  val document1: DOCUMENTREFERENCE = DOCUMENTREFERENCE(`fhir_id` = "1", `data_type` = "ALIR", `biospecimen_reference` = Seq("BIO1") , `files` = Seq(FILE(`file_name` = "file1.cram", `file_format` = "CRAM")), `dataset` = Some("dataset1"))
  val document7: DOCUMENTREFERENCE = DOCUMENTREFERENCE(`fhir_id` = "7", `data_type` = "ALIR", `biospecimen_reference` = Seq("BIO1"), `relates_to` = Some("1") , `files` = Seq(FILE(`file_name` = "file1.crai", `file_format` = "CRAI")), `dataset` = Some("dataset1"))
  val document2: DOCUMENTREFERENCE = DOCUMENTREFERENCE(`files` = Seq(FILE()), `dataset` = Some("dataset1"))
  val document3: DOCUMENTREFERENCE = DOCUMENTREFERENCE(`fhir_id` = "2", `data_type` = "SNV", `biospecimen_reference` = Seq("BIO1"), `files` = Seq(FILE(`file_name` = "file2.vcf", `file_format` = "VCF")), `dataset` = Some("dataset1"))
  val document4: DOCUMENTREFERENCE = DOCUMENTREFERENCE(`fhir_id` = "4", `data_type` = "GSV", `files` = Seq(FILE(`file_name` = "file4.vcf", `file_format` = "VCF")), `dataset` = Some("dataset1"))
  val document5: DOCUMENTREFERENCE = DOCUMENTREFERENCE(`fhir_id` = "3", `data_type` = "GCNV", `files` = Seq(FILE(`file_name` = "file3.vcf", `file_format` = "VCF")), `dataset` = Some("dataset1"))
  val document12: DOCUMENTREFERENCE = DOCUMENTREFERENCE(`fhir_id` = "12", `data_type` = "Annotated-SNV", `files` = Seq(FILE(`file_name` = "file12.vep", `file_format` = "VCF")), `dataset` = Some("dataset1"))
  val document6: DOCUMENTREFERENCE = DOCUMENTREFERENCE(`fhir_id` = "6", `participant_id` = "PRT0000002", `data_type` = "GCNV", `files` = Seq(FILE(`file_name` = "file6.vcf", `file_format` = "VCF")), `dataset` = Some("dataset2"))

  val diagnosis1: DIAGNOSIS_INPUT =DIAGNOSIS_INPUT()
  val diagnosis2: DIAGNOSIS_INPUT =DIAGNOSIS_INPUT(`subject` = "PRT0000002", `fhir_id` = "DIA0000002", `diagnosis_source_text` = "Tinnitus", `diagnosis_ICD_code` = "H93.19", `age_at_diagnosis` = "Old")
  val diagnosis3: DIAGNOSIS_INPUT =DIAGNOSIS_INPUT(`subject` = "PRT0000001", `fhir_id` = "DIA0000003", `diagnosis_source_text` = "Eczema",`diagnosis_mondo_code` = "MONDO:0004980", `diagnosis_ICD_code` = "L20.9", `age_at_diagnosis` = "Old")

  val phenotype1: PHENOTYPE = PHENOTYPE()
  val phenotype2: PHENOTYPE = PHENOTYPE(`fhir_id` = "PHE0000002", `phenotype_source_text` = "Hypertension", `phenotype_HPO_code` = PHENOTYPE_HPO_CODE(`code` = "HP:0000822"), `cqdg_participant_id` = "PRT0000002", `phenotype_observed` = "POS")
  val phenotype3: PHENOTYPE = PHENOTYPE(`fhir_id` = "PHE0000003", `phenotype_source_text` = "Hypertension", `phenotype_HPO_code` = PHENOTYPE_HPO_CODE(`code` = "HP:0000822"), `cqdg_participant_id` = "PRT0000001", `phenotype_observed` = "NEG")


  val biospecimen1: BIOSPECIMEN_INPUT = BIOSPECIMEN_INPUT(`fhir_id` = "BIO1", `subject` = "PRT0000001")
  val biospecimen2: BIOSPECIMEN_INPUT = BIOSPECIMEN_INPUT(`fhir_id` = "BIO2", `subject` = "PRT0000002") //no specimen

  val sample1: SAMPLE_INPUT = SAMPLE_INPUT(`fhir_id` = "SAMPLE1", `parent` = "BIO1", `subject` = "PRT0000001")
  val sample2: SAMPLE_INPUT = SAMPLE_INPUT(`fhir_id` = "SAMPLE2", `parent` = "BIO1", `subject` = "PRT0000001")
  val sample3: SAMPLE_INPUT = SAMPLE_INPUT(`fhir_id` = "SAMPLE3", `parent` = "BIO1", `subject` = "PRT0000001")
  val sample4: SAMPLE_INPUT = SAMPLE_INPUT(`fhir_id` = "SAMPLE4", `parent` = "BIO1", `subject` = "PRT0000001")

  val group1: GROUP = GROUP(`internal_family_id` = "12345STU0000001", `family_members` = Seq("PRT0000001", "PRT0000002", "PRT0000003"), `submitter_family_id` = "12345", `study_id` = "STU0000001")
  val group2: GROUP = GROUP(`internal_family_id` = "Group1Excluded", `family_members` = Seq("PRT0000004", "PRT0000005", "PRT0000006"), `submitter_family_id` = "Excluded1")

  val studyDataset: Seq[DATASET_INPUT] = Seq(DATASET_INPUT(`name` = "dataset1", `description` = None), DATASET_INPUT(`name` = "dataset2", `description` = Some("desc")))


  "transform" should "prepare index program_centric" in {
    val data: Map[String, DataFrame] = Map(
      "normalized_list" -> Seq(LIST_INPUT()).toDF(),
    )

    val output = new ProgramCentric()(conf).transform(data)

    output.keys should contain("es_index_program_centric")

    val program_centric = output("es_index_program_centric")

    val programCentricOutput = PROGRAM_CENTRIC(`contacts` = Seq(
      PROGRAM_CONTACT(`name` = null, `institution` = "RI-MUHC", `role_en` = null, `role_fr` = null,
        `picture_url` = null, `email` = "info@rare.quebec", `website` = "https://rare.quebec")
    ))

    program_centric.as[PROGRAM_CENTRIC].collect() should contain theSameElementsAs
      Seq(programCentricOutput)
  }

}
