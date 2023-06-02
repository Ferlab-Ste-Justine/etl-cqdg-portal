package bio.ferlab.fhir.etl

import bio.ferlab.datalake.spark3.SparkApp

object Enrich extends SparkApp {
  println(s"ARGS: " + args.mkString("[", ", ", "]"))
  val Array(_, _, jobName, studyIds) = args

  implicit val (conf, _, spark) = init()

  spark.sparkContext.setLogLevel("WARN")

  private val studies = studyIds.split(",").toList

  jobName match {
    case "specimen" => new SpecimenEnricher(studies).run()
    case "all" =>
      new SpecimenEnricher(studies).run()
  }
}
