package model

case class DOCUMENTREFERENCE(
                              `study_id`: String = "STU0000001",
                              `fhir_id`: String = "5",
                              `participant_id`: String = "PRT0000001",
                              `biospecimen_reference`: String = "SAM0000001",
                              `data_type`: String = "SSUP",
                              `relates_to`: Option[String] = None,
                              `data_category`: String = "Genomics",
                              `security`: String = "U",
                              `files`: Seq[FILE] = Seq(FILE()),
                              `dataset`: Option[String] = Some("Dataset1"),
                            )


case class FILE(
                 `file_name`: String = "file5.json",
                 `file_format`: String = "TGZ",
                 `file_size`: Long = 56,
                 `ferload_url`: String = "http://flerloadurl/outputPrefix/bc3aaa2a-63e4-4201-aec9-6b7b41a1e64a",
               )
