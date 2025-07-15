package bio.ferlab.fhir.etl

import bio.ferlab.fhir.etl.Publisher.{generateRegexCurrentAlias, generateRegexDesiredIndex, retrieveIndexesFromRegex}
import org.slf4j.LoggerFactory
import pureconfig.ConfigSource
import pureconfig.generic.auto._
import mainargs.{main, arg, ParserForMethods}

import scala.util.{Failure, Try}

case class ServiceConf(esConfig: Map[String, String])

object PublishTask {
  @main
  def publish(
               @arg(name = "es-port", short = 'p', doc = "Es port") esPort: String,
               @arg(name = "es-nodes", short = 'n', doc = "Es Nodes") esNodes: String,
               @arg(name = "release-id", short = 'r', doc = "Release ID") releaseId: String,
               @arg(name = "job-types", short = 'j', doc = "List of jobs separated by ,") jobTypes: String,
               @arg(name = "study_ids", short = 's', doc = "List of studies Id separated by ,") study_ids: Option[String],
             ): Unit = {
    val log = LoggerFactory.getLogger("publish")

    println(s"ARGS: $esNodes, $releaseId, $jobTypes, ${study_ids.getOrElse("")}")


    val serviceConf: ServiceConf = ConfigSource.resources(s"application.conf").loadOrThrow[ServiceConf]
    val esConf = serviceConf.esConfig

    implicit val esClient: EsHttpClient = new EsHttpClient(esConf)

    val studyList = study_ids.map(s => s.split(",").map(_.toLowerCase).toSeq)
    val jobs = jobTypes.split(",").toSeq
    println("before retrieveIndexesFromRegex")
    println(s"jobs: ${jobs}")
    println(s"studyList: ${studyList}")
    println(s"esNodes: ${esNodes}")
    val oldIndices = retrieveIndexesFromRegex(generateRegexCurrentAlias(jobs, studyList), "aliases")(esNodes, esPort)
    val desiredIndices = retrieveIndexesFromRegex(generateRegexDesiredIndex(jobs, releaseId, studyList), "indices")(esNodes, esPort)
    println("before retrieveIndexesFromRegex")
    println(oldIndices)
    println(desiredIndices)

    val results =
      studyList match {
        //Clinical
        case Some(studies) => jobs.flatMap(job => {
          studies.map(study => {
            Result(job, Some(study), Try {
              // only remove if a new index AND a current index exists for this study/job
              if(desiredIndices.exists(e => e.startsWith(s"${job}_$study")) &&
                oldIndices.exists(e => e.startsWith(s"${job}_$study"))){
                Publisher.updateAlias(job, s"${job}_$study*", "remove")(esNodes, esPort)
              }

              desiredIndices.find(e => e.startsWith(s"${job}_$study")).map(e => {
                Publisher.updateAlias(job, e, "add")(esNodes, esPort)
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
                Publisher.updateAlias(job, s"$job*", "remove")(esNodes, esPort)
              }

              desiredIndices.filter(e => e.contains(job)).map(e => {
                Publisher.updateAlias(job, e, "add")(esNodes, esPort)
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

  }

  def main(args: Array[String]): Unit = ParserForMethods(this).runOrExit(args)


  private case class Result[T](job: String, studyId: Option[String], t: Try[T])
}
