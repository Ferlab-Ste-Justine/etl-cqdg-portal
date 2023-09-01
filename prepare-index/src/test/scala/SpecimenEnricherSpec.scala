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
        FAMILY_RELATIONSHIP_NEW(internal_family_relationship_id = "FAMO1", focus_participant_id = "P1", submitter_participant_id = "P1", relationship_to_proband = "is_proband"),
        FAMILY_RELATIONSHIP_NEW(internal_family_relationship_id = "FAMO1", focus_participant_id = "P1", submitter_participant_id = "P2", relationship_to_proband = "father"),
        FAMILY_RELATIONSHIP_NEW(internal_family_relationship_id = "FAMO1", focus_participant_id = "P1", submitter_participant_id = "P3", relationship_to_proband = "mother")
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

    val output = new SpecimenEnricher(List("SD_Z6MWD3H0"))(conf).transform(data)

    val resultDF = output("enriched_specimen")

    val specimensEnriched = resultDF.as[SPECIMEN_ENRICHED].collect()
    val outputFamily = Seq(
      FAMILY_RELATIONSHIPS_ENRICHED(`participant_id` = "P1" ,`submitter_participant_id` = "P1_internal", `is_affected` = Some(true)),
      FAMILY_RELATIONSHIPS_ENRICHED(`participant_id` = "P2", `submitter_participant_id` = "P2_internal", `relationship_to_proband` = "father", `is_affected` = Some(false)),
      FAMILY_RELATIONSHIPS_ENRICHED(`participant_id` = "P3", `submitter_participant_id` = "P3_internal", `relationship_to_proband` = "mother", `is_affected` = Some(false)),
    )

    specimensEnriched.find(_.`biospecimen_id` == "FHIR_BS_1") shouldBe Some(
      SPECIMEN_ENRICHED(`biospecimen_id` = "FHIR_BS_1", `age_biospecimen_collection` = AGE_AT(17174), `submitter_biospecimen_id` = "BS_1",
        `participant` = PARTICIPANT_ENRICHED(`participant_id` = "P1",`fhir_id` = "P1", `gender` = "male", `submitter_participant_id` = "P1_internal", `family_relationships` = outputFamily, `relationship_to_proband` = "is_proband", `is_affected` = Some(true), `age_of_death` = Some(12)),
        `sample_id` = "SAMPLE1", `fhir_sample_id` = "FHIR_SAMPLE1")
    )
    specimensEnriched.find(_.`biospecimen_id` == "FHIR_BS_2") shouldBe Some(SPECIMEN_ENRICHED(
      `age_biospecimen_collection` = AGE_AT(17174),
      `participant` = PARTICIPANT_ENRICHED(`family_relationships` = outputFamily, `is_affected` = Some(false), `age_of_death` = Some(12)),
    ))
    specimensEnriched.find(_.`biospecimen_id` == "FHIR_BS_3") shouldBe Some(
      SPECIMEN_ENRICHED(`biospecimen_id` = "FHIR_BS_3", `age_biospecimen_collection` = AGE_AT(17174), `submitter_biospecimen_id` = "BS_3",
        `participant` = PARTICIPANT_ENRICHED(`participant_id` = "P3",`fhir_id` = "P3", `submitter_participant_id` = "P3_internal", `family_relationships` = outputFamily, `relationship_to_proband` = "mother", `is_affected` = Some(false), `age_of_death` = Some(12)),
        `sample_id` = null, `sample_type` = null, `fhir_sample_id` = null)
    )
  }

  //    ClassGenerator
  //      .writeCLassFile(
  //        "bio.ferlab.etl.prepare-index.model",
  //        "SPECIMEN_ENRICHED",
  //        resultDF,
  //        "prepare-index/src/test/scala/")
}

