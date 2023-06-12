package bio.ferlab.etl.normalize

import bio.ferlab.datalake.spark3.SparkApp

object Normalize extends SparkApp {
  println(s"ARGS: " + args.mkString("[", ", ", "]"))

  val Array(_, _, jobName, studyId) = args

  implicit val (conf, _, spark) = init()

  spark.sparkContext.setLogLevel("WARN")

  jobName match {
    case "snv" => new SNV(studyId).run()
    case "consequences" => new Consequences(studyId).run()
  }

}
