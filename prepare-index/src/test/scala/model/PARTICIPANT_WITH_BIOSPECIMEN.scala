package model

case class PARTICIPANT_WITH_BIOSPECIMEN(
                                         `participant_id`: String = "PRT0486300",
                                         `participant_2_id`: String = "PRT0486300",
                                         `sex`: String = "female",
                                         `age_at_recruitment`: String = "Young",
                                         `ethnicity`: String = "European",
                                         `submitter_participant_id`: String = "35849428444",
                                         `age_of_death`: String = "Old",
                                         `cause_of_death`: String = null,
                                         `is_affected`: String = null,
//                                         `diagnosis`: Seq[DIAGNOSIS] = Seq.empty,
//                                         `icd_tagged`: Seq[PHENOTYPE_TAGGED] = Seq.empty,
//                                         `mondo_tagged`: Seq[PHENOTYPE_TAGGED] = Seq.empty,
//                                         `observed_phenotype_tagged`: Seq[PHENOTYPE_TAGGED] = Seq.empty,
//                                         `non_observed_phenotype_tagged`: Seq[PHENOTYPE_TAGGED] = Seq.empty,
//                                         `familyRelationships`: FAMILY = null,
//                                         `is_a_proband`: Boolean = false,
//                                         `family_type`: String = "probant_only",
//                                         `is_proband`: Boolean = false, // TODO
                                         `biospecimens`: Set[BIOSPECIMEN] = Set.empty
                                       )
