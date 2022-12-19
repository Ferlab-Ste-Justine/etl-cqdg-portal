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
                          `access_limitations`: Seq[String] = Seq("DUO:0000005"),
                          `access_requirements`: Seq[String] = Seq("DUO:0000019", "DUO:0000021", "DUO:0000025", "DUO:0000026", "DUO:0000027", "DUO:0000029"),
                          `population`: String = "Adult",
                          `study_version`: String = "1",
                        )

case class CONTACT(
                    `type`: String = "url",
                    `value`: String = "https://sdas.cartagene.qc.ca",
                  )
