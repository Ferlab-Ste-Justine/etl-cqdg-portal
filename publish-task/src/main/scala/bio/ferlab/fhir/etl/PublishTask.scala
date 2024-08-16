package bio.ferlab.fhir.etl

import bio.ferlab.fhir.etl.Publisher.{generateRegexCurrentAlias, generateRegexDesiredIndex, retrieveIndexesFromRegex}
import org.slf4j.LoggerFactory
import pureconfig.ConfigSource
import pureconfig.generic.auto._

import scala.util.{Failure, Try}

case class ServiceConf(esConfig: Map[String, String])

object PublishTask extends App {
  val log = LoggerFactory.getLogger("publish")
  println(s"ARGS: " + args.mkString("[", ", ", "]"))

  val esNodes = args(0)
  val esPort = args(1)
  private val release_id = args(2)
  private val jobTypes = args(3)

  private val study_ids = args.length match {
    case x if x >= 5 => Some(args(4))
    case _ => None
  }


  private val serviceConf: ServiceConf = ConfigSource.resources(s"application.conf").loadOrThrow[ServiceConf]
  private val esConf = serviceConf.esConfig

  implicit val esClient: EsHttpClient = new EsHttpClient(esConf)

  private val studyList = study_ids.map(s => s.split(",").map(_.toLowerCase).toSeq)
  private val jobs = jobTypes.split(",").toSeq

  private val oldIndices = retrieveIndexesFromRegex(generateRegexCurrentAlias(jobs, studyList), "aliases")
  private val desiredIndices = retrieveIndexesFromRegex(generateRegexDesiredIndex(jobs, release_id, studyList), "indices")

  private val results =
    studyList match {
      //Clinical
      case Some(studies) => jobs.flatMap(job => {
        studies.map(study => {
          Result(job, Some(study), Try {
            // only remove if a new index AND a current index exists for this study/job
            if(desiredIndices.exists(e => e.startsWith(s"${job}_$study")) &&
              oldIndices.exists(e => e.startsWith(s"${job}_$study"))){
              Publisher.updateAlias(job, s"${job}_$study*", "remove")
            }

            desiredIndices.find(e => e.startsWith(s"${job}_$study")).map(e => {
              Publisher.updateAlias(job, e, "add")
            })
          })
        })
      })

      //Variants
      case None =>
        jobs.map(job => {
          Result(job, None, Try {
            // only remove if a new index AND a current index exists for this study/job
            if(desiredIndices.exists(_.startsWith(job)) && oldIndices.exists(_.startsWith(job))){
              Publisher.updateAlias(job, s"$job*", "remove")
            }

            desiredIndices.filter(e => e.contains(job)).map(e => {
              Publisher.updateAlias(job, e, "add")
            })
          })
        })
    }

  if (results.forall(_.t.isSuccess)) {
    System.exit(0)
  } else {
    results.collect { case Result(job, studyId, Failure(exception)) =>
      studyId match {
        case Some(id) => log.error(s"An error occur for study $id, job $job", exception)
        case None => log.error(s"An error occur job $job", exception)
      }

    }
    System.exit(-1)
  }

  private case class Result[T](job: String, studyId: Option[String], t: Try[T])
}
