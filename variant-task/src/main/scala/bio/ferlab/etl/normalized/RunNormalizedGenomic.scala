package bio.ferlab.etl.normalized

import bio.ferlab.datalake.commons.config.RuntimeETLContext
import mainargs.{ParserForMethods, arg, main}

object RunNormalizedGenomic {
  @main
  def snv(rc: RuntimeETLContext,
          @arg(name = "study-id", short = 's', doc = "Study Id") studyId: String,
          @arg(name = "study-code", short = 'c', doc = "Study Code") studyCode: String,
          @arg(name = "owner", short = 'o', doc = "Owner") owner: String,
          @arg(name = "dataset", short = 'd', doc = "Dataset") dataset: String,
          @arg(name = "batch", short = 'b', doc = "Batch") batch: String,
          @arg(name = "release-id", short = 'r', doc = "Release Id") releaseId: String,
          @arg(name = "reference-genome-path", short = 'g', doc = "Reference Genome Path") referenceGenomePath: Option[String]): Unit = SNV(rc, studyId, studyCode, owner, dataset, batch, releaseId, referenceGenomePath).run()


  @main
  def consequences(rc: RuntimeETLContext,
                   @arg(name = "study-id", short = 's', doc = "Study Id") studyId: String,
                   @arg(name = "study-code", short = 'c', doc = "Study Code") studyCode: String,
                   @arg(name = "owner", short = 'o', doc = "Owner") owner: String,
                   @arg(name = "dataset", short = 'd', doc = "Dataset") dataset: String,
                   @arg(name = "batch", short = 'b', doc = "Batch") batch: String,
                   @arg(name = "reference-genome-path", short = 'g', doc = "Reference Genome Path") referenceGenomePath: Option[String]): Unit = Consequences(rc, studyId, studyCode, owner, dataset, batch, referenceGenomePath).run()

  def main(args: Array[String]): Unit = ParserForMethods(this).runOrThrow(args, allowPositional = true)
}
