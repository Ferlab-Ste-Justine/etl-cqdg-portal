package bio.ferlab.etl.prepared

import bio.ferlab.datalake.commons.config.RuntimeETLContext
import bio.ferlab.datalake.spark3.genomics.prepared.VariantCentric
import mainargs.{ParserForMethods, main}

object RunPrepared {
  @main
  def variant_centric(rc: RuntimeETLContext): Unit = VariantCentric(rc).run()

  def main(args: Array[String]): Unit = ParserForMethods(this).runOrThrow(args, allowPositional = true)
}