package model

case class FILE_WITH_BIOSPECIMEN(
                                  `file_id`: Option[String] = Some("FIL0086557"),
                                  `file_2_id`: Option[String] = Some("FIL0086557"),
                                  `biospecimen_reference`: Option[String] = Some("SAM0247817"),
                                  `data_type`: Option[String] = Some("Germline Structural Variant"),
                                  `data_category`: Option[String] = Some("Genomics"),
                                  `dataset`: Option[String] = Some("Dataset1"),
                                  `relates_to`: Option[String] = None,
                                  `sequencing_experiment`: Option[SEQUENCING_EXPERIMENT_SINGLE] = None,
                                  `file_name`: Option[String] = Some("NS.1885.IDT_i7_87---IDT_i5_87.11137230.sv.vcf.gz"),
                                  `file_format`: Option[String] = Some("VCF"),
                                  `file_size`: Option[Double] = Some(1118934.0),
                                  `ferload_url`: Option[String] = Some("https://ferload.qa.cqdg.ferlab.bio/e24d78edeff9e033dac8445d32835c46c480d8a4|NWQ3MDA3NGY2YzAwMmFiOWE1YzdiZDVmNTFlZmU5YTcgIE5TLjE4ODUuSURUX2k3Xzg3LS0tSURUX2k1Xzg3LjExMTM3MjMwLnN2LnZjZi5neg=="),
                                  `biospecimens`: Seq[BIOSPECIMEN] = Seq.empty
                                )

case class FILE_WITH_BIOSPECIMEN_FACET_IDS(
                          file_fhir_id_1: Option[String] = Some("337786"),
                          file_fhir_id_2: Option[String] = Some("337786")
                        )
