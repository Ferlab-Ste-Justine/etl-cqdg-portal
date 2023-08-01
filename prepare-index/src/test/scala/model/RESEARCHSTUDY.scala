package model

case class RESEARCHSTUDY(
                          `fhir_id`: String = "STU0000001",
                          `keyword`: Seq[String] = Seq("genomics", "chronic conditions", "population-based cohort", "survey data"),
                          `release_id`: String = "5",
                          `study_id`: String = "STU0000001",
                          `description`: String = "CARTaGENE",
                          `study_code`: String = "cag",
                          `contact`: CONTACT = CONTACT(),
                          `status`: String = "completed",
                          `title`: String = "CARTaGENE",
                          `domain`: Seq[String] = Seq("General health"),
                          `access_limitations`: Seq[CODE_SYSTEM] = Seq(CODE_SYSTEM(`code` = "DUO:0000005", `display` = "General research use")),
                          `access_requirements`: Seq[CODE_SYSTEM] = Seq(CODE_SYSTEM(`code` = "DUO:0000021", `display` = "Ethics approval required"), CODE_SYSTEM(`code` = "DUO:0000027", `display` = "Project specific restriction")),
                          `population`: String = "Adult",
                          `study_version`: String = "1",
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
