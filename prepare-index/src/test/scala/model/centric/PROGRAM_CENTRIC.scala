package model.centric

case class PROGRAM_CENTRIC (
                           `program_id`: String =  "RARE-QC",
                           `name_en`: String = "RARE.Qc",
                           `name_fr`: String = "RARE.Qc – Le réseau pour avancer la recherche en maladies rares au Québec",
                           `studies`: Seq[String] = Seq("STU0000001"),
                           `description_fr`: String = "RARE.Qc est un réseau québécois",
                           `description_en`: String = "RARE.Qc is a Quebec-based.",
                           `managers`: Seq[CONTACT] = Seq(CONTACT()),
                           `contacts`: Seq[CONTACT] = Seq.empty[CONTACT],
                         )

case class CONTACT(
                    `name`: String = "Toto Tata",
                    `institution`: String = "CHU Sainte-Justine",
                    `role_en`: String = "Manager",
                    `role_fr`: String = "Gestionnaire",
                    `picture_url`: String = "/members_pictures/toto_tata.png",
                    `email`: String = null,
                    `website`: String = null,
                  )