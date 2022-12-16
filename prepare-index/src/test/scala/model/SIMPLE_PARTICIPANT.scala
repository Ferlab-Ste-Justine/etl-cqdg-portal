package model


case class SIMPLE_PARTICIPANT(
                               `participant_id`: String = "PRT0483458",
                               `gender`: String = "male",
                               `age_at_recruitment`: Int = 24566,
                               `vital_status`: String = "Unknown",
                               `ethnicity`: String = "European",
                               `submitter_participant_id`: String = "35849428444",
                               `age_of_death`: Int = 12,
                               `cause_of_death`: String = null,
                               `is_affected`: String = null,
                               `study_id`: String = "STU0000001",
                               `release_id`: String = "5",
                               `diagnoses`: Seq[DIAGNOSIS] = Seq.empty,
                               `icd_tagged`: Seq[PHENOTYPE_TAGGED] = Seq.empty,
                               `mondo_tagged`: Seq[PHENOTYPE_TAGGED] = Seq.empty,
                               `mondo`: Seq[PHENOTYPE_ENRICHED] = Seq.empty,
                               `observed_phenotype_tagged`: Seq[PHENOTYPE_TAGGED] = Seq.empty,
                               `non_observed_phenotype_tagged`: Seq[PHENOTYPE_TAGGED] = Seq.empty,
                               `observed_phenotypes`: Seq[PHENOTYPE_ENRICHED] = Seq.empty,
                               `familyRelationships`: FAMILY_RELATIONSHIP = FAMILY_RELATIONSHIP(),
                               `is_a_proband`: Boolean = false,
                             )
