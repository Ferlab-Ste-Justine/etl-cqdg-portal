package model


case class PARTICIPANT_CENTRIC(
                                `participant_id`: String = "PRT0494378",
                                `study_id`: String = "STU0000001",
                                `release_id`: String = "5",
                                `gender`: String = "male",
                                `vital_status`: Option[Boolean] = None,
                                `age_at_recruitment`: Int = 24566,
                                `ethnicity`: String = "European",
                                `submitter_participant_id`: String = "35849428444",
                                `age_of_death`: Int = 12,
                                `cause_of_death`: String = null,
                                `is_affected`: String = null,
                                `diagnoses`: Seq[DIAGNOSIS] = Seq.empty,
                                `icd_tagged`: Seq[PHENOTYPE_TAGGED] = Seq.empty,
                                `mondo_tagged`: Seq[PHENOTYPE_TAGGED] = Seq.empty,
                                `mondo`: Seq[PHENOTYPE_ENRICHED] = Seq.empty,
                                `observed_phenotype_tagged`: Seq[PHENOTYPE_TAGGED] = Seq.empty,
                                `non_observed_phenotype_tagged`: Seq[PHENOTYPE_TAGGED] = Seq.empty,
                                `observed_phenotypes`: Seq[PHENOTYPE_ENRICHED] = Seq.empty,
                                `familyRelationships`: FAMILY_RELATIONSHIP = FAMILY_RELATIONSHIP(),
                                `is_a_proband`: Boolean = false,
                                `study`: STUDY_CENTRIC = STUDY_CENTRIC(),
                                `files`: Seq[FILE_WITH_BIOSPECIMEN] = Seq.empty,
                              )

case class PARTICIPANT_FACET_IDS(
                                 participant_fhir_id_1: String = "38734",
                                 participant_fhir_id_2: String = "38734"
                               )
