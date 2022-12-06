package model

case class BIOSPECIMEN(
                        `fhir_id`: String = "BIO0041635",
                        `biospecimen_tissue_source`: String = "NCIT:C12434",
                        `age_biospecimen_collection`: AGE_AT = AGE_AT(),
                        `submitter_biospecimen_id`: String = "cag_sp_20832",
                        `sample_id`: String = "sam1",
                        `sample_type`: String = "NCIT:C449",
                        `submitter_participant_id`: String = "35849414972"
                      )

case class BIOSPECIMEN_FACET_IDS(
                                  biospecimen_fhir_id_1: String = "336842",
                                  biospecimen_fhir_id_2: String = "336842"
                                )
case class SAMPLE(
                   fhir_id: String = "sam1",
                   sample_type: String = "NCIT:C449",
                   submitter_participant_id: String = "35849414972"
                 )
