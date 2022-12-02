package model

case class DIAGNOSIS(
                      `fhir_id`: String = "DIA0021349",
                      `subject`: String = "PRT0000001",
                      `diagnosis_source_text`: String = "Rheumatoid arthritis",
                      `diagnosis_mondo_code`: String = "MONDO:0008383",
                      `diagnosis_ICD_code`: String = "M06.9",
                      `age_at_diagnosis`: Int = 12,
                    )

case class AGE_AT_DIAGNOSIS(
                             value: Long = 12,
                             unit: String = "days",
                           )
