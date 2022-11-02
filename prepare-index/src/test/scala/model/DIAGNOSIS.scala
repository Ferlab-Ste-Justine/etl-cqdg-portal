package model

case class DIAGNOSIS(
                      `subject`: String = "PRT0000003",
                      `study_id`: String = "STU0000001",
                      `release_id`: String = "5",
                      `fhir_id`: String = "DIA0000001",
                      `diagnosis_source_text`: String = "Hypercholesterolemia",
                      `diagnosis_mondo_code`: String = null,
                      `diagnosis_ICD_code`: String = "E78.3",
                      `age_at_diagnosis`: AGE_AT_DIAGNOSIS = AGE_AT_DIAGNOSIS(),
                    )

case class AGE_AT_DIAGNOSIS(
                             value: Long = 12,
                             unit: String = "days",
                           )
