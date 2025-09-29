package model.input

import model.CODEABLE

case class BIOSPECIMEN_INPUT(
                        `fhir_id`: String = "BIO0036882",
                        `subject`: String = "PRT0504459",
                        `study_id`: String = "STU0000001",
                        `security`: String = "U",
                        `biospecimen_tissue_source`: CODE_SYSTEM_INPUT = CODE_SYSTEM_INPUT(`code` = "NCIT:C12434"),
                        `cancer_anatomic_location`: CODE_SYSTEM_INPUT_TEXT = CODE_SYSTEM_INPUT_TEXT(`code` = "NCIT:C12434", `text` = Some("location")),
                        `tumor_histological_type`: CODE_SYSTEM_INPUT_TEXT = CODE_SYSTEM_INPUT_TEXT(`display`= Some("Missing - Not Provided"), `system`= "https://fhir.cqdg.ca/CodeSystem/cqdg-specimen-missing-codes", `code` = "Missing - Not provided", `text` = Some("histological_type5")),
                        `cancer_biospecimen_type`: Option[CODEABLE] = Some(CODEABLE()),
                        `tumor_normal_designation`: String = "Not applicable",
                        `age_biospecimen_collection`: String = "Young",
                        `submitter_biospecimen_id`: String = "cag_sp_20832",
                      )

case class SAMPLE_INPUT(
                       `subject`: String = "PRT0504459",
                       `parent`: String = "BIO0036882",
                       `study_id`: String = "STU0000001",
                       `fhir_id`: String = "SAM0252957",
                       `sample_type`: CODE_SYSTEM_INPUT = CODE_SYSTEM_INPUT(),
                       `submitter_sample_id`: String = "35849414972",
                     )

case class CODE_SYSTEM_INPUT (
                               `display`: Option[String] = None,
                               `system`: String = "http://purl.obolibrary.org/obo/ncit.owl",
                               `code`: String = "NCIT:C449",
                             )

case class CODE_SYSTEM_INPUT_TEXT (
                                    `display`: Option[String] = None,
                                    `system`: String = "http://purl.obolibrary.org/obo/ncit.owl",
                                    `code`: String = "NCIT:C449",
                                    `text`: Option[String] = None,
                                  )

