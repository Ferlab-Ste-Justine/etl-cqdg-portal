package bio.ferlab.etl.normalized

import bio.ferlab.datalake.commons.config.RuntimeETLContext
import mainargs.{ParserForMethods, arg, main}

object RunNormalizedGenomic {
  @main
  def snv(
      rc: RuntimeETLContext,
      @arg(name = "study-code", short = 's', doc = "Study Code") studyCode: String,
      @arg(name = "owner", short = 'o', doc = "Owner") owner: String,
      @arg(name = "dataset", short = 'd', doc = "Dataset") dataset: String,
      @arg(name = "batch", short = 'b', doc = "Batch") batch: String,
      @arg(name = "reference-genome-path", short = 'g', doc = "Reference Genome Path") referenceGenomePath: Option[
        String
      ]
  ): Unit = SNV(rc, studyCode, owner, dataset, batch, referenceGenomePath).run()

  @main
  def consequences(
      rc: RuntimeETLContext,
      @arg(name = "study-code", short = 's', doc = "Study Code") studyCode: String,
      @arg(name = "owner", short = 'o', doc = "Owner") owner: String,
      @arg(name = "dataset", short = 'd', doc = "Dataset") dataset: String,
      @arg(name = "batch", short = 'b', doc = "Batch") batch: String,
      @arg(name = "reference-genome-path", short = 'g', doc = "Reference Genome Path") referenceGenomePath: Option[
        String
      ]
  ): Unit = Consequences(rc, studyCode, owner, dataset, batch, referenceGenomePath).run()

  def main(args: Array[String]): Unit =
    ParserForMethods(this).runOrThrow(args, allowPositional = true)
}
