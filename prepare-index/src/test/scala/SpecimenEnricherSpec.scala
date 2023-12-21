import bio.ferlab.datalake.testutils.TestETLContext
import bio.ferlab.fhir.etl.SpecimenEnricher
import model._
import org.apache.spark.sql.DataFrame
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class SpecimenEnricherSpec extends AnyFlatSpec with Matchers with WithSparkSession with WithTestConfig {
  import spark.implicits._


  "transform" should "enrich specimen" in {
    val data: Map[String, DataFrame] = Map(
      "normalized_patient" -> Seq(
        PATIENT_INPUT(fhir_id = "P1", `submitter_participant_id` = "P1_internal"),
        PATIENT_INPUT(fhir_id = "P2", `submitter_participant_id` = "P2_internal", `gender` = "female"),
        PATIENT_INPUT(fhir_id = "P3", `submitter_participant_id` = "P3_internal", `gender` = "female"),
      ).toDF(),
      "normalized_family_relationship" -> Seq(
        FAMILY_RELATIONSHIP_NEW(internal_family_relationship_id = "FAMRO1", focus_participant_id = "P1", submitter_participant_id = "P1", relationship_to_proband = "Proband"),
        FAMILY_RELATIONSHIP_NEW(internal_family_relationship_id = "FAMRO1", focus_participant_id = "P1", submitter_participant_id = "P2", relationship_to_proband = "Father"),
        FAMILY_RELATIONSHIP_NEW(internal_family_relationship_id = "FAMRO1", focus_participant_id = "P1", submitter_participant_id = "P3", relationship_to_proband = "Mother")
      ).toDF(),
      "normalized_group" -> Seq(
        GROUP(internal_family_id = "FAMO1", family_members = Seq("P1", "P2", "P3")),
      ).toDF(),
      "normalized_sample_registration" -> Seq(
        SAMPLE_INPUT(`fhir_id` = "FHIR_SAMPLE1", subject = "P1", parent = "FHIR_BS_1", `submitter_sample_id` = "SAMPLE1"),
        SAMPLE_INPUT(`fhir_id` = "FHIR_SAMPLE2", `subject` = "P2", parent = "FHIR_BS_2", `submitter_sample_id` = "SAMPLE2"),
      ).toDF(),
      "normalized_biospecimen" -> Seq(
        BIOSPECIMEN_INPUT(fhir_id = "FHIR_BS_1", subject = "P1", `submitter_biospecimen_id` = "BS_1"),
        BIOSPECIMEN_INPUT(fhir_id = "FHIR_BS_2", subject = "P2", `submitter_biospecimen_id` = "BS_2"),
        BIOSPECIMEN_INPUT(fhir_id = "FHIR_BS_3", subject = "P3", `submitter_biospecimen_id` = "BS_3")
      ).toDF(),
      "normalized_research_study" -> Seq(
        RESEARCHSTUDY(`study_code` = "study_code1"),
      ).toDF(),
      "normalized_diagnosis" -> Seq(
        DIAGNOSIS_INPUT(),
      ).toDF(),
      "normalized_disease_status" -> Seq(
        DISEASE_STATUS(`fhir_id` = "F1", `subject` = "P1", `disease_status` = "yes"),
        DISEASE_STATUS(`fhir_id` = "F2", `subject` = "P2",  `disease_status` = null),
        DISEASE_STATUS(`fhir_id` = "F3", `subject` = "P3", `disease_status` = "no"),
      ).toDF()
    )

    val output = SpecimenEnricher(TestETLContext(), Seq("SD_Z6MWD3H0")).transform(data)

    val resultDF = output("enriched_specimen")

    val specimensEnriched = resultDF.as[SPECIMEN_ENRICHED].collect()

    specimensEnriched.find(_.`biospecimen_id` == "FHIR_BS_1") shouldBe Some(
      SPECIMEN_ENRICHED(`biospecimen_id` = "FHIR_BS_1", `age_biospecimen_collection` = "Young", `submitter_biospecimen_id` = "BS_1",
        `participant_id` = "P1",
        `participant_fhir_id` = "P1",
        `submitter_participant_id` = "P1_internal",
        `gender` = "male", `is_affected` = Some(true),
        `sample_id` = "SAMPLE1", `fhir_sample_id` = "FHIR_SAMPLE1",
        is_a_proband = Some(true),
        `mother_id` = Some("P3"), `father_id` = Some("P2")
      )
    )
    specimensEnriched.find(_.`biospecimen_id` == "FHIR_BS_2") shouldBe Some(SPECIMEN_ENRICHED(
      `age_biospecimen_collection` = "Young", `is_affected` = Some(false)
    ))
    specimensEnriched.find(_.`biospecimen_id` == "FHIR_BS_3") shouldBe Some(
      SPECIMEN_ENRICHED(`biospecimen_id` = "FHIR_BS_3", `age_biospecimen_collection` = "Young", `submitter_biospecimen_id` = "BS_3",
        `participant_id` = "P3",`participant_fhir_id` = "P3", `submitter_participant_id` = "P3_internal", `is_affected` = Some(false),
        `sample_id` = null, `sample_type` = null, `fhir_sample_id` = null)
    )

//    ClassGenerator.writeClassFile(
//        "bio.ferlab.etl.prepare-index.model",
//        "SPECIMEN_ENRICHED2",
//        resultDF,
//        "prepare-index/src/test/scala/")
  }


}

