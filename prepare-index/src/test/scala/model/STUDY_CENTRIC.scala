package model

case class LIGHT_STUDY_CENTRIC (
                                 `fhir_id`: String = "42776",
                                 `study_name`: String = "Kids First: Genomic Analysis of Congenital Heart Defects and Acute Lymphoblastic Leukemia in Children with Down Syndrome",
                                 `status`: String = "completed",
                                 `attribution`: String = "phs002330.v1.p1",
                                 `external_id`: String = "phs002330",
                                 `version`: String = "v1.p1",
                                 `investigator_id`: String = "123456",
                                 `study_id`: String = "SD_Z6MWD3H0",
                                 `study_code`: String = "KF-CHDALL",
                                 `program`: String = "Kids First",
                                 data_category: Seq[String] = Seq("Genomics"),
                                 `experimental_strategy`: Seq[String] = Seq("WGS"),
                                 `controlled_access`: Seq[String] = Seq("Controlled"),
                                 `participant_count`: Long = 0,
                                 `file_count`: Long = 0,
                                 `family_count`: Long = 0,
                                 `family_data`: Boolean = false,
                                 `biospecimen_count`: Long = 0,
                               )

case class STUDY_CENTRIC (
                           `internal_study_id`: String = "STU0000001",
                           `keyword`: Seq[String] = Seq("genomics", "chronic conditions", "population-based cohort", "survey data"),
                           `description`: String = "CARTaGENE",
                           `contact`: CONTACT = CONTACT(),
                           `status`: String = "completed",
                           `title`: String = "CARTaGENE",
                           `domain`: Seq[String] = Seq("Cancer"),
                           `data_access_codes`: DATA_ACCESS_CODES = DATA_ACCESS_CODES(),
                           `file_count`: Int = 1,
                           `participant_count`: Int = 1,
//                           `family_count`: Int = 1,
                           `family_data`: Boolean = true,
                           `study_version`: String = "1",
                         )
case class DATA_ACCESS_CODES(
                              access_limitations: Seq[String] = Seq("DUO:0000005"),
                              access_requirements: Seq[String] = Seq("DUO:0000019", "DUO:0000021")
                            )
