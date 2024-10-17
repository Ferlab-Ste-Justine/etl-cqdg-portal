package model

case class RESEARCHSTUDY(
                          `fhir_id`: String = "STU0000001",
                          `keyword`: Seq[String] = Seq("genomics", "chronic conditions", "population-based cohort", "survey data"),
                          `study_id`: String = "STU0000001",
                          `description`: String = "CARTaGENE",
                          `study_code`: String = "cag",
                          `access_authority`: CONTACT = CONTACT(),
                          `contact_names`: Seq[String] = Seq("contact1", "contact2"),
                          `contact_institutions`: Seq[String] = Seq("contact_institution1", "contact_institution2"),
                          `contact_emails`: Seq[String] = Seq("contact_eamil1@toto.com", "contact_eamil2@toto.com"),
                          `citation_statement`: String = "some_citation_statement",
                          `selection_criteria`: String = "some_selection_criteria",
                          `funding_sources`: Seq[String] = Seq("funding_source1", "funding_source2"),
                          `expected_number_participants`: String = "12",
                          `expected_number_biospecimens`: String = "15",
                          `expected_number_files`: String = "16",
                          `restricted_number_participants`: String = "2",
                          `restricted_number_biospecimens`: String = "3",
                          `restricted_number_files`: String = "3",
                          `data_categories`: Seq[String] = Seq("Genomics", "Proteomics", "Transcriptomics"), //TODO
                          `study_designs`: Seq[String] = Seq("case_only", "registry"),
                          `data_collection_methods`: Seq[String] = Seq("medical_records", "investigator_assessment"),
                          `status`: String = "completed",
                          `title`: String = "CARTaGENE",
                          `domain`: Seq[String] = Seq("General health"),
                          `access_limitations`: Seq[CODE_SYSTEM] = Seq(CODE_SYSTEM(`code` = "DUO:0000005", `display` = "General research use")),
                          `access_requirements`: Seq[CODE_SYSTEM] = Seq(CODE_SYSTEM(`code` = "DUO:0000021", `display` = "Ethics approval required"), CODE_SYSTEM(`code` = "DUO:0000027", `display` = "Project specific restriction")),
                          `population`: String = "Adult",
                          `study_version`: String = "1",
                          `security`: String = "U",
                          `data_sets`: Seq[DATASET_INPUT] = Seq(DATASET_INPUT()),
                        )

case class CONTACT(
                    `type`: String = "url",
                    `value`: String = "https://sdas.cartagene.qc.ca",
                  )

case class DATASET_INPUT(
                    `name`: String = "dataset1",
                    `description`: Option[String] = Some("bla bla")
                  )
