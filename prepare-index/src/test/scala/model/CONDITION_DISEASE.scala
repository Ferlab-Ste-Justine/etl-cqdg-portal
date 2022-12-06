package model

case class CONDITION_CODING(
                             `category`: String = "ICD",
                             `code`: String = "C91.0"
                           )

case class AGE_AT_EVENT(
                         `value`: Int = 0,
                         `unit`: String = "day",
                         `from_event`: String = "Birth"
                       )

case class DISEASE_STATUS(
                         `study_id`: String = "STU0000001",
                         `release_id`: String = "5",
                         `fhir_id`: String = "D_STATUS_ID",
                         `subject`: String = "SUBJECT",
                         `disease_status`: String = "DS",
                       )
