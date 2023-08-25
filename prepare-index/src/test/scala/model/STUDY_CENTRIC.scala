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
                                 `data_category`: Seq[String] = Seq("Genomics"),
                                 `experimental_strategy`: Seq[String] = Seq("WGS"),
                                 `controlled_access`: Seq[String] = Seq("Controlled"),
                                 `participant_count`: Long = 0,
                                 `file_count`: Long = 0,
                                 `family_count`: Long = 0,
                                 `family_data`: Boolean = false,
                                 `biospecimen_count`: Long = 0,
                               )

case class STUDY_CENTRIC (
                           `keyword`: Seq[String] = Seq("genomics", "chronic conditions", "population-based cohort", "survey data"),
                           `description`: String = "CARTaGENE",
                           `contact`: CONTACT = CONTACT(),
                           `name`: String = "CARTaGENE",
                           `domain`: Seq[String] = Seq("General health"),
                           `population`: String = "Adult",
                           `study_version`: String = "1",
                           `study_code`: String = "cag",
                           `study_id`: String = "STU0000001",
                           `data_types`: Seq[(String, String)] = Seq(("SSUP","1"), ("SNV","1"), ("GCNV","1"), ("ALIR","1"), ("GSV","1")),
                           `data_categories`: Seq[(String, String)] = Seq(("Genomics","1")),
                           `participant_count`: Int = 1,
                           `file_count`: Int = 5,
                           `status`: String = "completed",
                           `family_count`: Int = 1,
                           `sample_count`: Int = 1,
                           `experimental_strategies`: Seq[String] = Seq("WXS"),
                           `family_data`: Boolean = true,
                           `data_access_codes`: ACCESS_REQUIREMENTS = ACCESS_REQUIREMENTS(),
                           `datasets`: Seq[DATASET] = Nil,
                           `security`: String = "R"
                         )

case class STUDY_LIGHT (
                         `study_code`: String = "cag",
                         `name`: String = "CARTaGENE",
                         `population`: String = "Adult",
                         `domain`: Seq[String] = Seq("General health"),
                         `data_access_codes`: ACCESS_REQUIREMENTS = ACCESS_REQUIREMENTS(),
                         `contact`: CONTACT = CONTACT(),
                         )

case class DATA_ACCESS_CODES(
                              `access_limitations`: Seq[String] = Seq("DUO:0000005"),
                              `access_requirements`: Seq[String] = Seq("DUO:0000019", "DUO:0000021")
                            )

case class ACCESS_REQUIREMENTS(
                              `access_requirements`: Seq[String] = Seq("Ethics approval required (DUO:0000021)", "Project specific restriction (DUO:0000027)"),
                              `access_limitations`: Seq[String] = Seq("General research use (DUO:0000005)")
                              )

case class DATASET(
                    `name`: String = "name 1",
                    `description`: Option[String] = None,
                    `data_type`: Seq[String] = Seq("SNV"),
                    `experimental_strategy`: Seq[String] = Seq("WGS"),
                    `participant_count`: Int = 100,
                    `file_count`: Int = 100,
                  )
