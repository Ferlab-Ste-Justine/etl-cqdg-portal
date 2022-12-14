package model

case class BIOSPECIMEN_INPUT(
                        `fhir_id`: String = "BIO0036882",
                        `subject`: String = "PRT0504459",
                        `study_id`: String = "STU0000001",
                        `release_id`: String = "5",
                        `biospecimen_tissue_source`: String = "NCIT:C12434",
                        `age_biospecimen_collection`: AGE_AT_COLLECTION = AGE_AT_COLLECTION(),
                        `submitter_biospecimen_id`: String = "cag_sp_20832",
                      )

case class AGE_AT_COLLECTION (
                               `value`: Long = 17174,
                               `unit`: String = "days",
                             )

case class SAMPLE_INPUT(
                       `subject`: String = "PRT0504459",
                       `parent`: String = "BIO0036882",
                       `study_id`: String = "STU0000001",
                       `release_id`: String = "5",
                       `fhir_id`: String = "SAM0252957",
                       `sample_type`: CODE_SYSTEM = CODE_SYSTEM(),
                       `submitter_sample_id`: String = "35849414972",
                     )

case class CODE_SYSTEM (
                         `system`: String = "http://purl.obolibrary.org/obo/ncit.owl",
                         `code`: String = "NCIT:C449",
                       )
