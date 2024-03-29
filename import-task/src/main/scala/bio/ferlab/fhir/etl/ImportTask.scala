package bio.ferlab.fhir.etl

import bio.ferlab.datalake.spark3.SparkApp
import bio.ferlab.fhir.etl.fhavro.FhavroToNormalizedMappings

object ImportTask extends SparkApp {

  println(s"ARGS: " + args.mkString("[", ", ", "]"))

  val Array(_, _, studyIds) = args

  val studyList = studyIds.split(",").toList

  implicit val (conf, _, spark) = init()

  val jobs = FhavroToNormalizedMappings
    .mappings()
    .map { case (src, dst, transformations) => new ImportRawToNormalizedETL(src, dst, transformations, studyList) }

  jobs.foreach(_.run())
}
