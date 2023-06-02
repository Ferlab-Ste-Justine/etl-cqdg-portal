package bio.ferlab.etl.normalize

import bio.ferlab.datalake.spark3.SparkApp

object Normalize extends SparkApp {
  println(s"ARGS: " + args.mkString("[", ", ", "]"))

  //TODO should pass only study code and not studyName? need to split in MINIO... (studyName: WGS-EE => studyId: ST0000042)
  val Array(_, _, jobName, studyName, studyId, prefix) = args

  implicit val (conf, _, spark) = init()

  spark.sparkContext.setLogLevel("WARN")

  jobName match {
    case "snv" => new SNV(prefix, studyName, studyId).run()
    case "consequences" => new Consequences(prefix, studyName).run()
  }

}
