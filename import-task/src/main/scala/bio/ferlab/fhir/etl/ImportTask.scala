package bio.ferlab.fhir.etl

import bio.ferlab.datalake.spark3.SparkApp
import bio.ferlab.fhir.etl.fhavro.FhavroToNormalizedMappings

object ImportTask extends SparkApp {

  println(s"ARGS: " + args.mkString("[", ", ", "]"))

  val Array(_, _, releaseId, studyIds) = args

  val studyList = studyIds.split(",").toList

  implicit val (conf, _, spark) = init()

  val jobs = FhavroToNormalizedMappings
    .mappings(releaseId)
    .map { case (src, dst, transformations) => new ImportRawToNormalizedETL(src, dst, transformations, releaseId, studyList) }

//    jobs.map(_.extract()).foreach(e => e.foreach(r => r._2.show(false)))
//    jobs.foreach(_.run().foreach(e => e._2.printSchema()))
    jobs.foreach(_.run().foreach(e => e._2.show(false)))
}
