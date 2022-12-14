package model

case class BIOSPECIMEN_CENTRIC(
                                `biospecimen_id`: String = "BIO0031886",
                                `study_id`: String = "STU0000001",
                                `release_id`: String = "5",
                                `biospecimen_tissue_source`: String = "NCIT:C12434",
                                `age_biospecimen_collection`: Long = 19044,
                                `submitter_biospecimen_id`: String = "cag_sp_00076",
                                `study`: STUDY_CENTRIC = STUDY_CENTRIC(),
                                `participant`: SIMPLE_PARTICIPANT = SIMPLE_PARTICIPANT(),
                                `files`: Seq[FILE_WITH_SEQ_EXPERIMENT] = Nil,
                                `sample_id`: String = "SAM0258735",
                                `sample_type`: String = "NCIT:C449",
                                `submitter_sample_id`: String = "11132816"
                              )
