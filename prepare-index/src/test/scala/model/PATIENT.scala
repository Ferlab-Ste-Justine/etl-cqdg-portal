package model

case class PATIENT(
                    `study_id`: String = "STU0000001",
                    `participant_id`: String = "PRT0483534",
                    `fhir_id`: String = "PRT0483534",
                    `sex`: String = "male",
                    `deceasedBoolean`: Boolean = false,
                    `age_at_recruitment`: String = "Young",
                    `ethnicity`: String = "European",
                    `submitter_participant_id`: String = "35849427674",
                    `age_of_death`: String = null,
                    `is_affected`: String = null,
                  )

case class PATIENT_INPUT(
                    `study_id`: String = "STU0000001",
                    `fhir_id`: String = "PRT0483534",
                    `sex`: String = "male",
                    `vital_status`: String = "Unknown",
                    `age_at_recruitment`: String = "Young",
                    `ethnicity`: String = "European",
                    `submitter_participant_id`: String = "35849427674",
                    `security`: String = "U",
                    `age_of_death`: String = "Old",
                  )


case class CAUSE_OF_DEATH(
                           `fhir_id`: String = "PRT0000003",
                           `study_id`: String = "STU0000001",
                           `submitter_participant_ids`: String = "PRT0000003",
                           `cause_of_death`: String = "Pie eating",
                         )

//TODO RENAME
case class FAMILY_RELATIONSHIP_NEW (
                           `study_id`: String = "STU0000001",
                           `internal_family_relationship_id`: String = "FAM0000001FR",
                           `category`: String = "SOCIALHISTORY",
                           `submitter_participant_id`: String = "PRT0000001",
                           `focus_participant_id`: String = "PRT0000003",
                           `relationship_to_proband`: String = "Mother",
                         )
case class PATIENT_OUPUT(
                    `submitter_participant_ids`: String = "PRT0000001",
                    `sex`: String = "female",
                    `vital_status`: String = "Unknown",
                    `age_at_recruitment`: String = "215574198069",
                    `ethnicity`: String = "french canadian",
                    `submitter_participant_id`: String = "35849409716",
                    `age_of_death`: String = null,
                    `family_relationships`: Seq[FAMILY_RELATIONSHIP_OUTPUT] = Seq(FAMILY_RELATIONSHIP_OUTPUT())
                  )

case class FAMILY_RELATIONSHIP_OUTPUT(
                          `internal_family_id`: String = "12345STU0000001",
                          `submitter_family_id`: String = "12345",
                          `submitter_participant_id`: String = "PRT0000001",
                          `focus_participant_id`: String = "PRT0000003",
                          `relationship_to_proband`: String = "Mother",
                          `family_type`: String = "Case-parent trio",
                        )
