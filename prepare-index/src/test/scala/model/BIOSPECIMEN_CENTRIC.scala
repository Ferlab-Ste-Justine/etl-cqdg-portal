package model

import model.input.{CODE_SYSTEM_INPUT, CODE_SYSTEM_INPUT_TEXT}

case class BIOSPECIMEN_CENTRIC(
                                `biospecimen_id`: String = "BIO0031886",
                                `study_id`: String = "STU0000001",
                                `security`: String = "U",
                                `cancer_anatomic_location`: CODE_SYSTEM_TEXT = CODE_SYSTEM_TEXT(`text` = Some("location")),
                                `tumor_histological_type`: CODE_SYSTEM_TEXT = CODE_SYSTEM_TEXT(`display`= "Missing - Not Provided", `code` = "Missing - Not provided", `text` = Some("histological_type5")),
                                `cancer_biospecimen_type`: Option[String] = Some("NCIT:164032"),
                                `tumor_normal_designation`: String = "Not applicable",
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

case class CODE_SYSTEM_TEXT(
                                 `display`: String = "toto (NCIT:C6657)",
                                 `code`: String = "NCIT:C6657",
                                 `text`: Option[String] = Some("some text")
                               )