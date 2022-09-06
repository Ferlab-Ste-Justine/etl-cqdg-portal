package bio.ferlab.fhir.etl.model

object Environment extends Enumeration {

  type Environment = Value

  val CQDGDEV   = Value("cqdg-dev")
  val CADGCDEV   = Value("cqdg-dev")
  val CGDAQA    = Value("cqdg-qa")


  def fromString(s: String): Environment = values.find(_.toString.toUpperCase == s.toUpperCase) match {
    case Some(value) => value
    case _ => throw new NoSuchElementException(s"No value for $s")
  }
}
