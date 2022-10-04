package model

case class PATIENT(
                    `fhir_id`: String = "PRT0000001",
                    `study_id`: String = "STU0000001",
                    `release_id`: String = "5",
                    `gender`: String = "female",
                    `vital_status`: Boolean = false,
                    `age_at_recruitment`: AGE_AT_RECRUITMENT = AGE_AT_RECRUITMENT(),
                    `ethnicity`: String = "french canadian",
                    `submitter_participant_id`: String = "35849409716",
                    `age_of_death`: String = null,
                  )

case class AGE_AT_RECRUITMENT(
                               `value`: String = "215574198069",
                               `unit`: String = "days",
                             )
