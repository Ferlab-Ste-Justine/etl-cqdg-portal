package model


case class PARTICIPANT_CENTRIC(
                                `participant_id`: String = "PRT0494378",
                                `study_id`: String = "STU0000001",
                                `gender`: String = "Man",
                                `gender_another_category`: Option[String] = None,
                                `gender_collect_method`: String = "Self-identified",
                                `sex`: String = "Male",
                                `sex_another_category`: Option[String] = None,
                                `sex_collect_method`: String = "Clinician-recorded",
                                `race`: String = "White",
                                `race_another_racial_category`: Option[String] = None,
                                `race_collect_method`: String = "Self-identified",
                                `vital_status`: Option[String] = Some("Unknown"),
                                `age_at_recruitment`: String = "Young",
                                `ethnicity`: String = "European",
                                `submitter_participant_id`: String = "35849428444",
                                `age_of_death`: String = "Old",
                                `cause_of_death`: String = null,
                                `is_affected`: String = null,
                                `security`: String = "U",
                                `biospecimens`: Seq[BIOSPECIMEN] = Seq.empty,
                                `diagnoses`: Seq[DIAGNOSIS] = Seq.empty,
                                `icd_tagged`: Seq[PHENOTYPE_TAGGED] = Seq.empty,
                                `mondo_tagged`: Seq[PHENOTYPE_TAGGED] = Seq.empty,
                                `mondo`: Seq[PHENOTYPE_ENRICHED] = Seq.empty,
                                `observed_phenotype_tagged`: Seq[PHENOTYPE_TAGGED] = Seq.empty,
                                `observed_phenotypes`: Seq[PHENOTYPE_ENRICHED] = Seq.empty,
                                `family_relationships`: Seq[FAMILY_RELATIONSHIP_WITH_FAMILY] = Nil,
                                `is_a_proband`: Option[Boolean] = None,
                                `study`: STUDY_LIGHT = STUDY_LIGHT(),
                                `files`: Seq[FILE_WITH_BIOSPECIMEN] = Seq.empty,
                              )

case class PARTICIPANT_FACET_IDS(
                                 participant_fhir_id_1: String = "38734",
                                 participant_fhir_id_2: String = "38734"
                               )
