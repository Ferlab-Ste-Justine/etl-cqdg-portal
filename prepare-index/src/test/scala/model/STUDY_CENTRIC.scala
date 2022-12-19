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
                           `participant_count`: Int = 3,
                           `file_count`: Int = 6,
                           `status`: String = "completed",
                           `family_count`: Int = 1,
                           `experimental_strategy`: Seq[String] = Seq("WXS"),
                           `hpo_terms`: Seq[String] = Seq("Hypercholesterolemia (HP:0003124)", "Hypertension (HP:0000822)"),
                           `mondo_terms`: Seq[String] = Seq("atopic eczema (MONDO:0004980)"),
                           `icd_terms`: Seq[String] = Seq("Tinnitus, unspecified ear (H93.19)", "Atopic dermatitis, unspecified (L20.9)", "Hyperchylomicronemia (E78.3)"),
                           `family_data`: Boolean = true,
                           `data_access_codes`: ACCESS_REQUIREMENTS = ACCESS_REQUIREMENTS(),
                         )
case class DATA_ACCESS_CODES(
                              `access_limitations`: Seq[String] = Seq("DUO:0000005"),
                              `access_requirements`: Seq[String] = Seq("DUO:0000019", "DUO:0000021")
                            )

case class ACCESS_REQUIREMENTS(
                              `access_requirements`: Seq[String] = Seq("publication required (DUO:0000019)", "ethics approval required (DUO:0000021)", "time limit on use (DUO:0000025)", "user-specific restriction (DUO:0000026)", "project-specific restriction (DUO:0000027)", "return to database/resource (DUO:0000029)"),
                              `access_limitations`: Seq[String] = Seq("obsolete general research use and clinical care (DUO:0000005)")
                              )
