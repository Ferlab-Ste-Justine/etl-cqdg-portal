package model

case class FILE_CENTRIC(
                         file_id: String = "FIL0086557",
                         study_id: String = "STU0000001",
                         `security`: String = "R",
                         biospecimen_reference: String = "SAM0247817",
                         data_type: String = "Germline Structural Variant",
                         dataset: Option[String] = Some("Dataset1"),
                         `relates_to`: Option[RELATES_TO] = None,
                         data_category: String = "Genomics",
                         file_name: String = "NS.1885.IDT_i7_87---IDT_i5_87.11137230.sv.vcf.gz",
                         file_format: String = "VCF",
                         file_size: Double = 1118934.0,
                         ferload_url: String = "https://ferload.qa.cqdg.ferlab.bio/e24d78edeff9e033dac8445d32835c46c480d8a4|NWQ3MDA3NGY2YzAwMmFiOWE1YzdiZDVmNTFlZmU5YTcgIE5TLjE4ODUuSURUX2k3Xzg3LS0tSURUX2k1Xzg3LjExMTM3MjMwLnN2LnZjZi5neg==",
                         `biospecimens`: Set[BIOSPECIMEN] = Set.empty,
                         participants: Seq[PARTICIPANT_WITH_BIOSPECIMEN] = Seq.empty,
                         study: STUDY_LIGHT = STUDY_LIGHT(),
                         sequencing_experiment: SEQUENCING_EXPERIMENT_SINGLE = SEQUENCING_EXPERIMENT_SINGLE(),
                       )

case class FILE_FACET_IDS(
                           file_fhir_id_1: String = "337786",
                           file_fhir_id_2: String = "337786"
                         )

case class RELATES_TO(
                          file_id: String = "13",
                          file_name: String = "file.2",
                          file_format: String = "CRAI",
                          file_size: Long = 56,
                          ferload_url: String = "http://flerloadurl/outputPrefix/bc3aaa2a-63e4-4201-aec9-6b7b41a1e64a",
                         )
