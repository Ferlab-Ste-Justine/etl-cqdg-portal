import bio.ferlab.datalake.commons.config.{Configuration, ConfigurationLoader}
import bio.ferlab.datalake.spark3.loader.GenericLoader.read
import bio.ferlab.fhir.etl.centricTypes.StudyCentric
import model._
import org.apache.spark.sql.DataFrame
import org.scalatest.matchers.should.Matchers
import org.scalatest.flatspec.AnyFlatSpec

class StudyCentricSpec extends AnyFlatSpec with Matchers with WithSparkSession {

  import spark.implicits._

  implicit val conf: Configuration = ConfigurationLoader.loadFromResources("config/dev-cqdg.conf")

  val patient1: PATIENT =  PATIENT()
  val patient2: PATIENT =  PATIENT( `fhir_id` = "PRT0000002",`gender` = "male", `age_at_recruitment` = "215640979761", `submitter_participant_id` = "35849419216")
  val patient3: PATIENT =  PATIENT( `fhir_id` = "PRT0000003",`gender` = "male", `age_at_recruitment` = "215557091632", `ethnicity` = "aboriginal" ,`submitter_participant_id` = "35849430470", `age_of_death` = "3223600")

  val family1: FAMILY_RELATIONSHIP_NEW = FAMILY_RELATIONSHIP_NEW()
  val family2: FAMILY_RELATIONSHIP_NEW = FAMILY_RELATIONSHIP_NEW(`internal_family_relationship_id` = "FAM0000002FR", `submitter_participant_id` = "PRT0000002", `relationship_to_proband` = "Father")
  val family3: FAMILY_RELATIONSHIP_NEW = FAMILY_RELATIONSHIP_NEW(`internal_family_relationship_id` = "FAM0000003FR", `submitter_participant_id` = "PRT0000003", `relationship_to_proband` = "Is the proband")


  val document1: DOCUMENTREFERENCE = DOCUMENTREFERENCE(`fhir_id` = "1", `data_type` = "ALIR", `files` = Seq(FILE(`file_name` = "file1.cram", `file_format` = "CRAM"), FILE(`file_name` = "file1.crai", `file_format` = "CRAI")))
  val document2: DOCUMENTREFERENCE = DOCUMENTREFERENCE(`files` = Seq(FILE()))
  val document3: DOCUMENTREFERENCE = DOCUMENTREFERENCE(`fhir_id` = "2", `data_type` = "SNV", `files` = Seq(FILE(`file_name` = "file2.vcf", `file_format` = "VCF")))
  val document4: DOCUMENTREFERENCE = DOCUMENTREFERENCE(`fhir_id` = "4", `data_type` = "GSV", `files` = Seq(FILE(`file_name` = "file4.vcf", `file_format` = "VCF")))
  val document5: DOCUMENTREFERENCE = DOCUMENTREFERENCE(`fhir_id` = "3", `data_type` = "GCNV", `files` = Seq(FILE(`file_name` = "file3.vcf", `file_format` = "VCF")))

  val diagnosis1: DIAGNOSIS =DIAGNOSIS()
  val diagnosis2: DIAGNOSIS =DIAGNOSIS(`subject` = "PRT0000002", `fhir_id` = "DIA0000002", `diagnosis_source_text` = "Tinnitus", `diagnosis_ICD_code` = "H93.19", `age_at_diagnosis` = AGE_AT_DIAGNOSIS(value = 215556831))
  val diagnosis3: DIAGNOSIS =DIAGNOSIS(`subject` = "PRT0000001", `fhir_id` = "DIA0000003", `diagnosis_source_text` = "Eczema",`diagnosis_mondo_code` = "MONDO:0004980", `diagnosis_ICD_code` = "L20.9", `age_at_diagnosis` = AGE_AT_DIAGNOSIS(value = 48))

  val phenotype1: PHENOTYPE = PHENOTYPE()
  val phenotype2: PHENOTYPE = PHENOTYPE(`fhir_id` = "PHE0000002", `phenotype_source_text` = "Hypertension", `phenotype_HPO_code` = PHENOTYPE_HPO_CODE(`code` = "HP:0000822"), `cqdg_participant_id` = "PRT0000002", `phenotype_observed` = "POS")
  val phenotype3: PHENOTYPE = PHENOTYPE(`fhir_id` = "PHE0000003", `phenotype_source_text` = "Hypertension", `phenotype_HPO_code` = PHENOTYPE_HPO_CODE(`code` = "HP:0000822"), `cqdg_participant_id` = "PRT0000001", `phenotype_observed` = "NEG")


  "transform" should "prepare index study_centric" in {
    val data: Map[String, DataFrame] = Map(
      "normalized_research_study" -> Seq(RESEARCHSTUDY()).toDF(),
      "normalized_patient" -> Seq(patient1, patient2, patient3).toDF(),
      "normalized_document_reference" -> Seq(document1, document2, document3, document4, document5).toDF(),
      "normalized_family_relationship" -> Seq(family1, family2, family3).toDF(),
      "normalized_group" -> Seq(GROUP_NEW()).toDF(),
      "normalized_diagnosis" -> Seq(diagnosis1, diagnosis2, diagnosis3).toDF(),
      "normalized_task" -> Seq(TASK()).toDF(),
      "normalized_phenotype" -> Seq(phenotype1, phenotype2, phenotype3).toDF(),
      "hpo_terms" -> read(getClass.getResource("/hpo_terms.json").toString, "Json", Map(), None, None),
      "mondo_terms" -> read(getClass.getResource("/mondo_terms.json").toString, "Json", Map(), None, None),
      "icd_terms" -> read(getClass.getResource("/icd_terms.json").toString, "Json", Map(), None, None),
      "duo_terms" -> read(getClass.getResource("/duo_terms.csv").toString, "csv", Map("header" -> "true"), None, None),
    )

    val output = new StudyCentric("5", List("STU0000001"))(conf).transform(data)


    output.keys should contain("es_index_study_centric")

    val study_centric = output("es_index_study_centric")

    val studyCentricOutput = STUDY_CENTRIC()


    study_centric.as[STUDY_CENTRIC].collect() should contain theSameElementsAs
      Seq(studyCentricOutput)
  }

//  "transform" should "prepare inde study_centric with family_data false if no group" in {
//    val data: Map[String, DataFrame] = Map(
//      "normalized_research_study" -> Seq(RESEARCHSTUDY()).toDF(),
//      "normalized_patient" -> Seq(PATIENT(), PATIENT()).toDF(),
//      "normalized_document_reference" -> Seq(DOCUMENTREFERENCE(), DOCUMENTREFERENCE(), DOCUMENTREFERENCE()).toDF(),
//      "normalized_group" -> Seq[GROUP]().toDF(),
//      "normalized_specimen" -> Seq(BIOSPECIMEN()).toDF()
//    )
//
//    val output = new StudyCentric("re_000001", List("SD_Z6MWD3H0"))(conf).transform(data)
//
//    output.keys should contain("es_index_study_centric")
//
//    val study_centric = output("es_index_study_centric")
//
//    study_centric.as[STUDY_CENTRIC].collect() should contain theSameElementsAs
//      Seq(STUDY_CENTRIC())
//  }

}
