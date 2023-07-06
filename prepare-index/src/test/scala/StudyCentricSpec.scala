import bio.ferlab.datalake.commons.config.{Configuration, ConfigurationLoader, SimpleConfiguration}
import bio.ferlab.datalake.spark3.loader.GenericLoader.read
import bio.ferlab.fhir.etl.centricTypes.StudyCentric
import model._
import org.apache.spark.sql.DataFrame
import org.scalatest.matchers.should.Matchers
import org.scalatest.flatspec.AnyFlatSpec
import pureconfig.generic.auto._

class StudyCentricSpec extends AnyFlatSpec with Matchers with WithSparkSession {

  import spark.implicits._

  implicit val conf: Configuration = ConfigurationLoader.loadFromResources[SimpleConfiguration]("config/dev-cqdg.conf")

  val patient1: PATIENT_INPUT =  PATIENT_INPUT(`fhir_id` = "PRT0000001")
  val patient2: PATIENT_INPUT =  PATIENT_INPUT( `fhir_id` = "PRT0000002", `age_at_recruitment` = 12, `submitter_participant_id` = "35849419216")
  val patient3: PATIENT_INPUT =  PATIENT_INPUT( `fhir_id` = "PRT0000003", `age_at_recruitment` = 13, `ethnicity` = "aboriginal" ,`submitter_participant_id` = "35849430470", `age_of_death` = 3223600)

  val family1: FAMILY_RELATIONSHIP_NEW = FAMILY_RELATIONSHIP_NEW()
  val family2: FAMILY_RELATIONSHIP_NEW = FAMILY_RELATIONSHIP_NEW(`internal_family_relationship_id` = "FAM0000002FR", `submitter_participant_id` = "PRT0000002", `relationship_to_proband` = "Father")
  val family3: FAMILY_RELATIONSHIP_NEW = FAMILY_RELATIONSHIP_NEW(`internal_family_relationship_id` = "FAM0000003FR", `submitter_participant_id` = "PRT0000003", `relationship_to_proband` = "Is the proband")


  val document1: DOCUMENTREFERENCE = DOCUMENTREFERENCE(`fhir_id` = "1", `data_type` = "ALIR", `biospecimen_reference` = "BIO1" , `files` = Seq(FILE(`file_name` = "file1.cram", `file_format` = "CRAM"), FILE(`file_name` = "file1.crai", `file_format` = "CRAI")))
  val document2: DOCUMENTREFERENCE = DOCUMENTREFERENCE(`files` = Seq(FILE()))
  val document3: DOCUMENTREFERENCE = DOCUMENTREFERENCE(`fhir_id` = "2", `data_type` = "SNV", `biospecimen_reference` = "BIO1", `files` = Seq(FILE(`file_name` = "file2.vcf", `file_format` = "VCF")))
  val document4: DOCUMENTREFERENCE = DOCUMENTREFERENCE(`fhir_id` = "4", `data_type` = "GSV", `files` = Seq(FILE(`file_name` = "file4.vcf", `file_format` = "VCF")))
  val document5: DOCUMENTREFERENCE = DOCUMENTREFERENCE(`fhir_id` = "3", `data_type` = "GCNV", `files` = Seq(FILE(`file_name` = "file3.vcf", `file_format` = "VCF")))
  val document6: DOCUMENTREFERENCE = DOCUMENTREFERENCE(`fhir_id` = "6", `participant_id` = "PRT0000002", `data_type` = "GCNV", `files` = Seq(FILE(`file_name` = "file3.vcf", `file_format` = "VCF")))

  val diagnosis1: DIAGNOSIS_INPUT =DIAGNOSIS_INPUT()
  val diagnosis2: DIAGNOSIS_INPUT =DIAGNOSIS_INPUT(`subject` = "PRT0000002", `fhir_id` = "DIA0000002", `diagnosis_source_text` = "Tinnitus", `diagnosis_ICD_code` = "H93.19", `age_at_diagnosis` = AGE_AT(value = 215556831))
  val diagnosis3: DIAGNOSIS_INPUT =DIAGNOSIS_INPUT(`subject` = "PRT0000001", `fhir_id` = "DIA0000003", `diagnosis_source_text` = "Eczema",`diagnosis_mondo_code` = "MONDO:0004980", `diagnosis_ICD_code` = "L20.9", `age_at_diagnosis` = AGE_AT(value = 48))

  val phenotype1: PHENOTYPE = PHENOTYPE()
  val phenotype2: PHENOTYPE = PHENOTYPE(`fhir_id` = "PHE0000002", `phenotype_source_text` = "Hypertension", `phenotype_HPO_code` = PHENOTYPE_HPO_CODE(`code` = "HP:0000822"), `cqdg_participant_id` = "PRT0000002", `phenotype_observed` = "POS")
  val phenotype3: PHENOTYPE = PHENOTYPE(`fhir_id` = "PHE0000003", `phenotype_source_text` = "Hypertension", `phenotype_HPO_code` = PHENOTYPE_HPO_CODE(`code` = "HP:0000822"), `cqdg_participant_id` = "PRT0000001", `phenotype_observed` = "NEG")


  val biospecimen1: BIOSPECIMEN_INPUT = BIOSPECIMEN_INPUT(`fhir_id` = "BIO1", `subject` = "PRT0000001")
  val biospecimen2: BIOSPECIMEN_INPUT = BIOSPECIMEN_INPUT(`fhir_id` = "BIO2", `subject` = "PRT0000002") //no specimen

  val sample1: SAMPLE_INPUT = SAMPLE_INPUT(`fhir_id` = "SAMPLE1", `parent` = "BIO1", `subject` = "PRT0000001")
  val sample2: SAMPLE_INPUT = SAMPLE_INPUT(`fhir_id` = "SAMPLE2", `parent` = "BIO1", `subject` = "PRT0000001")
  val sample3: SAMPLE_INPUT = SAMPLE_INPUT(`fhir_id` = "SAMPLE3", `parent` = "BIO1", `subject` = "PRT0000001")
  val sample4: SAMPLE_INPUT = SAMPLE_INPUT(`fhir_id` = "SAMPLE4", `parent` = "BIO1", `subject` = "PRT0000001")


  "transform" should "prepare index study_centric" in {
    val data: Map[String, DataFrame] = Map(
      "normalized_research_study" -> Seq(RESEARCHSTUDY()).toDF(),
      "normalized_patient" -> Seq(patient1, patient2, patient3).toDF(),
      "normalized_document_reference" -> Seq(document1, document2, document3, document4, document5, document6).toDF(),
      "normalized_family_relationship" -> Seq(family1, family2, family3).toDF(),
      "normalized_group" -> Seq(GROUP_NEW()).toDF(),
      "normalized_diagnosis" -> Seq(diagnosis1, diagnosis2, diagnosis3).toDF(),
      "normalized_task" -> Seq(TASK()).toDF(),
      "normalized_phenotype" -> Seq(phenotype1, phenotype2, phenotype3).toDF(),
      "normalized_biospecimen" -> Seq(biospecimen1, biospecimen2).toDF(),
      "normalized_sample_registration" -> Seq(sample1, sample2, sample3, sample4).toDF(),
      "duo_terms" -> read(getClass.getResource("/duo_terms.csv").toString, "csv", Map("header" -> "true"), None, None),
    )

    val output = new StudyCentric("5", List("STU0000001"))(conf).transform(data)


    output.keys should contain("es_index_study_centric")

    val study_centric = output("es_index_study_centric")

    val studyCentricOutput = STUDY_CENTRIC(
      `participant_count` = 2, // PRT0000001, PRT0000002
      `data_types` = Seq(("SSUP","1"), ("SNV","1"), ("GCNV","2"), ("ALIR","1"), ("GSV","1")),
      `sample_count` = 4,
      `data_categories` = Seq(("Genomics","2")),
    )

    study_centric.as[STUDY_CENTRIC].collect() should contain theSameElementsAs
      Seq(studyCentricOutput)
  }

}
