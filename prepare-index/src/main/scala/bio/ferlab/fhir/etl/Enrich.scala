package bio.ferlab.fhir.etl

import bio.ferlab.datalake.commons.config.RuntimeETLContext
import bio.ferlab.fhir.etl.mainutils.Studies
import mainargs.{ParserForMethods, main}

object Enrich {
  @main
  def specimen(rc: RuntimeETLContext, studies: Studies): Unit = SpecimenEnricher(rc, studies.ids).run()


  def main(args: Array[String]): Unit = ParserForMethods(this).runOrThrow(args, allowPositional = true)
}
