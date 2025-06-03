package model

case class STUDY_CENTRIC (
                           `keyword`: Seq[String] = Seq("genomics", "chronic conditions", "population-based cohort", "survey data"),
                           `description`: String = "CARTaGENE",
                           `access_authority`: CONTACT = CONTACT(),
                           `contact_names`: Seq[String] = Seq("contact1", "contact2"),
                           `contact_institutions`: Seq[String] = Seq("contact_institution1", "contact_institution2"),
                           `contact_emails`: Seq[String] = Seq("contact_eamil1@toto.com", "contact_eamil2@toto.com"),
                           `principal_investigators`: Seq[String] = Seq("Batman2", "Superman2"),
                           `citation_statement`: String = "some_citation_statement",
                           `selection_criteria`: String = "some_selection_criteria",
                           `funding_sources`: Seq[String] = Seq("funding_source1", "funding_source2"),
                           `status`: String = "completed",
                           `name`: String = "CARTaGENE",
                           `website`: String = "http://study2.com",
                           `domain`: Seq[String] = Seq("General health"),
                           `study_code`: String = "cag",
                           `population`: String = "Adult",
                           `study_version`: String = "1",
                           `expected_number_participants`: String = "12",
                           `expected_number_biospecimens`: String = "15",
                           `expected_number_files`: String = "16",
                           `restricted_number_participants`: String = "2",
                           `restricted_number_biospecimens`: String = "3",
                           `restricted_number_files`: String = "3",
                           `data_types`: Seq[(String, String)] = Seq(("SSUP","1"), ("SNV","1"), ("GCNV","1"), ("ALIR","1"), ("GSV","1")),
                           `data_categories`: Seq[(String, String)] = Seq(("Genomics","2"), ("Proteomics", null), ("Transcriptomics", null)),
                           `study_designs`: Seq[String] = Seq("case_only", "registry"),
                           `data_collection_methods`: Seq[String] = Seq("medical_records", "investigator_assessment"),
                           `participant_count`: Int = 1,
                           `file_count`: Int = 5,
                           `family_count`: Int = 1,
                           `sample_count`: Int = 1,
                           `experimental_strategies_1`: Seq[(CODEABLE, String)] = Seq((CODEABLE("WXS"), "6")),
                           `experimental_strategies`: Seq[(String, String)] = Seq(("WXS", "6")),
                           `family_data`: Boolean = true,
                           `data_access_codes`: ACCESS_REQUIREMENTS = ACCESS_REQUIREMENTS(),
                           `study_id`: String = "STU0000001",
                           `datasets`: Seq[DATASET] = Nil,
                           `security`: String = "U"
                         )

case class STUDY_LIGHT (
                         `study_code`: String = "cag",
                         `name`: String = "CARTaGENE",
                         `population`: String = "Adult",
                         `domain`: Seq[String] = Seq("General health"),
                         `data_access_codes`: ACCESS_REQUIREMENTS = ACCESS_REQUIREMENTS(),
                         `access_authority`: CONTACT = CONTACT(),
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
                    `data_types`: Seq[String] = Seq("SNV"),
                    `experimental_strategies_1`: Seq[CODEABLE] = Seq(CODEABLE("WGS")),
                    `participant_count`: Int = 100,
                    `file_count`: Int = 100,
                  )
