package model

case class AGE_AT_EVENT(
                         `value`: Int = 0,
                         `unit`: String = "day",
                         `from_event`: String = "Birth"
                       )

case class DISEASE_STATUS(
                         `study_id`: String = "STU0000001",
                         `fhir_id`: String = "D_STATUS_ID",
                         `subject`: String = "SUBJECT",
                         `disease_status`: String = "DS",
                       )
