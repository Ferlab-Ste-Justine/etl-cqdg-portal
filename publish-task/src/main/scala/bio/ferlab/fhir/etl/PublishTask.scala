package bio.ferlab.fhir.etl

import bio.ferlab.fhir.etl.Publisher.retrievePreviousIndices
import org.slf4j.LoggerFactory
import pureconfig.ConfigSource
import pureconfig.generic.auto._

import scala.util.{Failure, Try}

case class ServiceConf(esConfig: Map[String, String])

object PublishTask extends App {
  val log = LoggerFactory.getLogger("publish")
  println(s"ARGS: " + args.mkString("[", ", ", "]"))

  val Array(
  esNodes, // http://localhost:9200
  esPort, // 9200
  release_id, // release id
  study_ids, // study ids separated by ,
  jobTypes, // study_centric or participant_centric or file_centric or biospecimen_centric or all. can be multivalue spearate by ,
  ) = args
  val serviceConf: ServiceConf = ConfigSource.resources(s"application.conf").loadOrThrow[ServiceConf]
  private val esConf = serviceConf.esConfig

  implicit val esClient: EsHttpClient = new EsHttpClient(esConf)


  private val studyList = study_ids.split(",")
  private val jobs = jobTypes.split(",").toSeq

  val oldIndices = retrievePreviousIndices(jobs, studyList)

  private val results = jobs.flatMap { job =>
    studyList.map(studyId => Result(job, studyId, Try {
      oldIndices.find(i => i.contains(job) && i.contains(studyId)).foreach(oldIndex => {
        Publisher.updateAlias(job, oldIndex, "remove")

        val newIndexName = s"${job}_${studyId}_$release_id".toLowerCase
        Publisher.updateAlias(job, newIndexName, "add")
      })
    })
    )
  }
  if (results.forall(_.t.isSuccess)) {
    System.exit(0)
  } else {
    results.collect { case Result(job, studyId, Failure(exception)) =>
      log.error(s"An error occur for study $studyId, job $job", exception)
    }
    System.exit(-1)
  }

  private case class Result[T](job: String, studyId: String, t: Try[T])
}
