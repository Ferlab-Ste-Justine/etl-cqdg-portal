package bio.ferlab.fhir.etl

import bio.ferlab.datalake.spark3.elasticsearch.ElasticSearchClient
import org.apache.hadoop.shaded.org.apache.http.client.methods.HttpGet
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
  env,
  project,
  release_id, // release id
  study_ids, // study ids separated by ,
  jobTypes, // study_centric or participant_centric or file_centric or biospecimen_centric or all. can be multivalue spearate by ,
  ) = args

  val serviceConf: ServiceConf = ConfigSource.resources(s"config/$env-$project.conf").loadOrThrow[ServiceConf]
  private val esConf = serviceConf.esConfig
  implicit val esClient: ElasticSearchClient = new ElasticSearchClient(s"$esNodes:$esPort", esConf.get("es.net.http.auth.user"), esConf.get("es.net.http.auth.pass"))

  private val studyList = study_ids.split(",")

  private val jobs = if (jobTypes == "all") Seq("biospecimen_centric", "participant_centric", "study_centric", "file_centric") else jobTypes.split(",").toSeq
  private val results = jobs.flatMap { job =>
    studyList.map(studyId => Result(job, studyId, Try {
      val newIndexName = s"${job}_${studyId}_$release_id".toLowerCase
      println(s"Add $newIndexName to alias $job")

      val oldIndexName = Publisher.retrievePreviousIndex(job, studyId, s"$esNodes:$esPort")
      oldIndexName.foreach(old => println(s"Remove $old from alias $job"))

      Publisher.publish(job, newIndexName, oldIndexName)
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
