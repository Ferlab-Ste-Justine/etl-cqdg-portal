package model

case class DIAGNOSIS(
                      `fhir_id`: String = "DIA0021349",
                      `study_id`: String = "STU0000001",
                      `release_id`: String = "5",
                      `subject`: String = "PRT0000001",
                      `diagnosis_source_text`: String = "Rheumatoid arthritis",
                      `diagnosis_mondo_code`: String = "MONDO:0008383",
                      `diagnosis_ICD_code`: String = "M06.9",
                      `age_at_diagnosis`: Int = 12,
                    )

case class DIAGNOSIS_INPUT(
                            `fhir_id`: String = "DIA0021349",
                            `study_id`: String = "STU0000001",
                            `release_id`: String = "5",
                            `subject`: String = "PRT0000001",
                            `diagnosis_source_text`: String = "Rheumatoid arthritis",
                            `diagnosis_mondo_code`: String = "MONDO:0008383",
                            `diagnosis_ICD_code`: String = "M06.9",
                            `age_at_diagnosis`: AGE_AT = AGE_AT(),
                    )

case class AGE_AT(
                             value: Long = 12,
                             unit: String = "days",
                           )
