import bio.ferlab.datalake.commons.config.{Configuration, ConfigurationLoader, SimpleConfiguration}
import bio.ferlab.datalake.spark3.loader.GenericLoader.read
import bio.ferlab.fhir.etl.centricTypes.StudyCentric
import model._
import model.input.LIST_INPUT
import org.apache.spark.sql.DataFrame
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class StudyCentricSpec extends AnyFlatSpec with Matchers with WithSparkSession {

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


  "transform" should "prepare index study_centric" in {
    val data: Map[String, DataFrame] = Map(
      "normalized_research_study" -> Seq(RESEARCHSTUDY(`data_sets` = studyDataset)).toDF(),
      "normalized_patient" -> Seq(patient1, patient2, patient3).toDF(),
      "normalized_document_reference" -> Seq(document1, document2, document3, document4, document5, document6, document12, document7).toDF(),
      "normalized_family_relationship" -> Seq(family1, family2, family3).toDF(),
      "normalized_group" -> Seq(group1, group2).toDF(),
      "normalized_diagnosis" -> Seq(diagnosis1, diagnosis2, diagnosis3).toDF(),
      "normalized_task" -> Seq(TASK()).toDF(),
      "normalized_list" -> Seq(LIST_INPUT()).toDF(),
      "normalized_phenotype" -> Seq(phenotype1, phenotype2, phenotype3).toDF(),
      "normalized_biospecimen" -> Seq(biospecimen1, biospecimen2).toDF(),
      "normalized_sample_registration" -> Seq(sample1, sample2, sample3, sample4).toDF(),
      "duo_terms" -> read(getClass.getResource("/duo_terms.csv").toString, "csv", Map("header" -> "true"), None, None),
    )

    val output = new StudyCentric(List("STU0000001"))(conf).transform(data)

    output.keys should contain("es_index_study_centric")

    val study_centric = output("es_index_study_centric")

    val studyCentricOutput = STUDY_CENTRIC(
      `participant_count` = 2, // PRT0000001, PRT0000002
      `data_types` = Seq(("SSUP","1"),("Annotated-SNV","1"), ("SNV","1"), ("GCNV","2"), ("ALIR","1"), ("GSV","1")),
      `sample_count` = 4,
      `file_count` = 7,
      `datasets` = Seq(
        // Dataset file_count should be 6 as CRAI files should be excluded
        DATASET(`name` = "dataset1", `data_types` = Seq("SNV", "GSV", "ALIR", "SSUP", "GCNV", "Annotated-SNV"), `experimental_strategies` = Seq("WXS"), `experimental_strategies_1` = Seq(CODEABLE("WXS")), `participant_count` = 1, `file_count` = 6),
        DATASET(`name` = "dataset2", `description` = Some("desc"), `data_types` = Seq("GCNV"), `experimental_strategies` = Nil, `experimental_strategies_1` = Nil, `participant_count` = 1, `file_count` = 1)
      )
    )

    study_centric.as[STUDY_CENTRIC].collect() should contain theSameElementsAs
      Seq(studyCentricOutput)
  }

}
