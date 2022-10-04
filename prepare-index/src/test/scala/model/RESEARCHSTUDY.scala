package model

case class RESEARCHSTUDY(
                          `fhir_id`: String = "STU0000001",
                          `keyword`: Seq[String] = Seq("genomics", "chronic conditions", "population-based cohort", "survey data"),
                          `description`: String = "CARTaGENE",
                          `contact`: CONTACT = CONTACT(),
                          `status`: String = "completed",
                          `title`: String = "CARTaGENE",
                          `domain`: Seq[DOMAIN] = Nil,
                          `access_limitations`: Seq[String] = Seq("DUO:0000005"),
                          `access_requirements`: Seq[String] = Seq("DUO:0000019", "DUO:0000021"),
                        )

case class CONTACT(
                    `type`: String = "url",
                    `value`: String = "https://sdas.cartagene.qc.ca",
                  )

case class DOMAIN(
                   `system`: String = "http://fhir.cqdg.ferlab.bio/CodeSystem/research-domain",
                   `code`: String = "General health",
                 )
