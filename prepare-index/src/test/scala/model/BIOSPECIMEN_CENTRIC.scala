package model

case class BIOSPECIMEN_CENTRIC(
                                `biospecimen_id`: String = "BIO0031886",
                                `study_id`: String = "STU0000001",
                                `security`: String = "U",
                                `biospecimen_tissue_source`: String = "Blood (NCIT:C12434)",
                                `age_biospecimen_collection`: String = "Young",
                                `submitter_biospecimen_id`: String = "cag_sp_00076",
                                `study`: STUDY_LIGHT = STUDY_LIGHT(),
                                `participant`: SIMPLE_PARTICIPANT = SIMPLE_PARTICIPANT(),
                                `files`: Seq[FILE_WITH_SEQ_EXPERIMENT] = Nil,
                                `sample_id`: String = "SAM0258735",
                                `sample_type`: String = "DNA (NCIT:C449)",
                                `submitter_sample_id`: String = "11132816"
                              )
