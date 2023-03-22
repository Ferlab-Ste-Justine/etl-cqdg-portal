package model

case class FILE_INPUT(
                       `study_id`: String = "STU0000001",
                       `release_id`: String = "5",
                       `fhir_id`: String = "FIL0081238",
                       `participant_id`: String = "PRT0503531",
                       `biospecimen_reference`: String = "SAM0234037",
                       `data_type`: String = "Germline CNV",
                       `data_category`: String = "Genomics",
                       `files`: Seq[FILE_ATTACHMENT] = Seq(FILE_ATTACHMENT()),
                       )

case class FILE_ATTACHMENT(
                            `file_name`: String = "NS.1681.IDT_i7_18---IDT_i5_18.11132871.cnv.vcf.gz",
                            `file_format`: String = "VCF",
                            `file_size`: String = "94273.0",
                            `ferload_url`: String = "https://ferload.qa.cqdg.ferlab.bio/7b21b84d6034cff9ee187c242fa21ae5be2164ef",
                            `file_hash`: String = "NTkyM2Q0ZGUyMzE0ZmI4M2I1ODA5NWNkMzU1Y2JiZWIgIE5TLjE2ODEuSURUX2k3XzE4LS0tSURUX2k1XzE4LjExMTMyODcxLmNudi52Y2YuZ3o=",
                          )

case class FILE_WITH_SEQ_EXPERIMENT(
                                     `file_name`: String = "NS.1681.IDT_i7_18---IDT_i5_18.11132871.cnv.vcf.gz",
                                     `file_format`: String = "VCF",
                                     `file_size`: String = "94273.0",
                                     `ferload_url`: String = "https://ferload.qa.cqdg.ferlab.bio/7b21b84d6034cff9ee187c242fa21ae5be2164ef",
                                     `file_id`: String = "FIL0081238",
                                     `biospecimen_reference`: String = "SAM0234037",
                                     `data_category`: String = "Genomics",
                                     `sequencing_experiment`: SEQUENCING_EXPERIMENT_SINGLE = SEQUENCING_EXPERIMENT_SINGLE(),
                          )

