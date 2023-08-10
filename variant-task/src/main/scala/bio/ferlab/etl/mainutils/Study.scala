package bio.ferlab.etl.mainutils

import mainargs.{ParserForClass, arg}

case class Study(@arg(name = "study-id", short = 'i', doc = "Study id") id: String)

object Study {
  implicit def configParser: ParserForClass[Study] = ParserForClass[Study]
}