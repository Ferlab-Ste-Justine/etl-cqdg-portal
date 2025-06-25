package bio.ferlab.fhir.etl

import bio.ferlab.datalake.spark3.SparkApp
import bio.ferlab.fhir.etl.centricTypes.{BiospecimenCentric, FileCentric, ParticipantCentric, ProgramCentric, SimpleParticipant, StudyCentric}
import org.apache.spark.sql.functions.col

object PrepareIndex extends SparkApp {
  println(s"ARGS: " + args.mkString("[", ", ", "]"))

  val Array(_, _, jobName, studyIds) = args

  implicit val (conf, _, spark) = init()

  spark.sparkContext.setLogLevel("WARN")

  val studyList = studyIds.split(",").toList

  val studyCentric = new StudyCentric(studyList).run()


  //Remove studies that are restricted
  val filteredStudies =
    studyCentric("es_index_study_centric")
      .where(col("security") =!= "R")
      .select("study_id").collect().map(r => r.getString(0)).toList

  new SimpleParticipant(filteredStudies).run()


  jobName match {
    case "study_centric" =>
    case "participant_centric" =>
      if (filteredStudies.nonEmpty) new ParticipantCentric(filteredStudies).run()
    case "file_centric" =>
      if (filteredStudies.nonEmpty) new FileCentric(filteredStudies).run()
    case "biospecimen_centric" =>
      if (filteredStudies.nonEmpty) new BiospecimenCentric(filteredStudies).run()
    case "all" =>
      if (filteredStudies.nonEmpty) {
        new ParticipantCentric(filteredStudies).run()
        new FileCentric(filteredStudies).run()
        new BiospecimenCentric(filteredStudies).run()
        new ProgramCentric(filteredStudies).run()
      }
  }
}
