package model


case class SIMPLE_PARTICIPANT(
                               `participant_id`: String = "PRT0483458",
                               `participant_2_id`: String = "PRT0483458",
                               `gender`: DEMOGRAPHICS = DEMOGRAPHICS(`code` = "Man" ,`collect_method` = CODEABLE(`code` = "Self-identified", `display` = null)),
                               `sex`: String = "Male",
                               `sex_at_birth`: DEMOGRAPHICS = DEMOGRAPHICS(`code` = "Male", `collect_method` = CODEABLE(`code` = "Clinician-recorded", `display` = null)),
                               `race`: DEMOGRAPHICS = DEMOGRAPHICS(`code` = "White", `collect_method` = CODEABLE(`code` = "Self-identified", `display` = null)),
                               `age_at_recruitment`: String = "Young",
                               `vital_status`: String = "Unknown",
                               `ethnicity`: String = "European",
                               `submitter_participant_id`: String = "35849428444",
                               `age_of_death`: String = "Old",
                               `cause_of_death`: String = null,
                               `is_affected`: String = null,
                               `study_id`: String = "STU0000001",
                               `security`: String = "U",
                               `diagnoses`: Seq[DIAGNOSIS] = Seq.empty,
                               `icd_tagged`: Seq[PHENOTYPE_TAGGED] = Seq.empty,
                               `mondo_tagged`: Seq[PHENOTYPE_TAGGED] = Seq.empty,
                               `mondo`: Seq[PHENOTYPE_ENRICHED] = Seq.empty,
                               `observed_phenotype_tagged`: Seq[PHENOTYPE_TAGGED] = Seq.empty,
                               `phenotypes_tagged`: Seq[PHENOTYPE_TAGGED_WITH_OBSERVED] = Seq.empty,
                               `observed_phenotypes`: Seq[PHENOTYPE_ENRICHED] = Seq.empty,
                               `family_relationships`: Seq[FAMILY_RELATIONSHIP_WITH_FAMILY] = Nil,
                               `is_a_proband`: Option[Boolean] = None,
                             )
