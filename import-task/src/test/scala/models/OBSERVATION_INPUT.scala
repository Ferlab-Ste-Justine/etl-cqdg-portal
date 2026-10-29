package models

case class OBSERVATION_INPUT(
    `study_id`: String = "study1",
    `fhir_id`: String = "PRT0000001",
    `code`: CODEABLE_CONCEPT_INPUT = CODEABLE_CONCEPT_INPUT(),
    `subject`: REFERENCE_INPUT = REFERENCE_INPUT()
)

case class CODEABLE_CONCEPT_INPUT(
    `coding`: Seq[CODING_INPUT] = Seq(CODING_INPUT())
)

case class CODING_INPUT(
    `system`: String = "https://fhir.cqdg.ca/CodeSystem/cause-of-death-codes",
    `code`: String = "Cancer"
)

case class REFERENCE_INPUT(
    `reference`: String = "Patient/PRT0000001"
)
