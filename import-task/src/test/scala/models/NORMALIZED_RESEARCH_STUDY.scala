/**
 * Generated by [[bio.ferlab.datalake.testutils.ClassGenerator]]
 * on 2023-08-11T19:42:23.966397
 */
package models




case class NORMALIZED_RESEARCH_STUDY(`fhir_id`: String = "CAG",
                                     `keyword`: Seq[String] = Seq("genomics", "chronic conditions", "population-based cohort", "survey data"),
                                     `description`: String = "CARTaGENE is a public research platform of the CHU Sainte-Justine created to accelerate health research. CARTaGENE consists of both biological samples and health data from 43,000 Québec residents aged between 40 to 69 years.",
                                     `access_authority`: ACCESS_AUTHORITY = ACCESS_AUTHORITY(),
                                     `contact_names`: Seq[String] = Seq("contact1", "contact2"),
                                     `contact_institutions`: Seq[String] = Seq("contact_institution1", "contact_institution2"),
                                     `contact_emails`: Seq[String] = Seq("contact_eamil1@toto.com", "contact_eamil2@toto.com"),
                                     `citation_statement`: String = "some_citation_statement",
                                     `selection_criteria`: String = "some_selection_criteria",
                                     `funding_sources`: Seq[String] = Seq("funding_source1", "funding_source2"),
                                     `status`: String = "completed",
                                     `title`: String = "CARTaGENE",
                                     `domain`: Seq[String] = Seq("general-health"),
                                     `study_code`: String = "CAG",
                                     `access_limitations`: Seq[ACCESS_LIMITATIONS] = Seq(ACCESS_LIMITATIONS()),
                                     `access_requirements`: Seq[ACCESS_REQUIREMENTS] = Seq(ACCESS_REQUIREMENTS(), ACCESS_REQUIREMENTS(`code` = "DUO:0000021"), ACCESS_REQUIREMENTS(`code` = "DUO:0000025"), ACCESS_REQUIREMENTS(`code` = "DUO:0000026"), ACCESS_REQUIREMENTS(`code` = "DUO:0000027"), ACCESS_REQUIREMENTS(`code` = "DUO:0000029")),
                                     `population`: String = "Adult",
                                     `study_version`: String = "1",
                                     `expected_number_participants`: String = "12",
                                     `expected_number_biospecimens`: String = "15",
                                     `expected_number_files`: String = "16",
                                     `restricted_number_participants`: String = "2",
                                     `restricted_number_biospecimens`: String = "3",
                                     `restricted_number_files`: String = "3",
                                     `data_categories`: Seq[String] = Seq("genomics", "proteomics"),
                                     `study_designs`: Seq[String] = Seq("case_only", "registry"),
                                     `data_collection_methods`: Seq[String] = Seq("medical_records", "investigator_assessment"),
                                     `data_sets`: Seq[DATA_SETS] = Seq(DATA_SETS(), DATA_SETS(`name` = "d2", `description`= "description 2")),
                                     `study_id`: String = "CAG",
                                     `security` : String = "R")

case class ACCESS_AUTHORITY(`type`: String = "url",
                   `value`: String = "https://sdas.cartagene.qc.ca")

case class ACCESS_LIMITATIONS(`code`: String = "DUO:0000005",
                              `display`: Option[String] = None)

case class ACCESS_REQUIREMENTS(`code`: String = "DUO:0000019",
                               `display`: Option[String] = None)

case class DATA_SETS(`name`: String = "d1",
                     `description`: String = "description 1")
