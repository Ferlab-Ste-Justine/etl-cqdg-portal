package model.centric

case class PROGRAM_CENTRIC (
                           `program_id`: String =  "RARE-QC",
                           `name_en`: String = "RARE.Qc",
                           `name_fr`: String = "RARE.Qc – Le réseau pour avancer la recherche en maladies rares au Québec",
                           `studies`: Seq[String] = Seq("STU0000001"),
                           `description_fr`: String = "RARE.Qc est un réseau québécois",
                           `description_en`: String = "RARE.Qc is a Quebec-based.",
                         )
