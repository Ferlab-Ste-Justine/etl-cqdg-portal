package model

case class DOCUMENTREFERENCE(
                              `study_id`: String = "STU0000001",
                              `release_id`: String = "5",
                              `fhir_id`: String = "3",
                              `participant_id`: String = "PRT0000001",
                              `biospecimen_reference`: String = "SAM0000001",
                              `data_type`: String = "GCNV",
                              `data_category`: String = "GENO",
                              `files`: Seq[FILE] = Nil,
                            )


case class FILE(
                 `file_name`: String = "file3.vcf",
                 `file_format`: String = "VCF",
                 `file_size`: Long = 57,
                 `ferload_url`: String = "http://flerloadurl/outputPrefix/8e4adfb6-9831-442d-b96b-198b0fc9589b",
               )
