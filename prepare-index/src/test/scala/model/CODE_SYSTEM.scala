package model

case class CODE_SYSTEM(
    `system`: String = "http://purl.obolibrary.org/obo/ncit.owl",
    `code`: String = "NCIT:C449",
    `display`: Option[String] = None
)
