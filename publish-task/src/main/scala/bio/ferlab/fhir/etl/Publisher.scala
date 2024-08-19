package bio.ferlab.fhir.etl

import org.apache.http.client.methods.{HttpGet, HttpPost}
import org.apache.http.entity.StringEntity

object Publisher {

  /**
   * Will generate the regex to fetch current (in alias) ElasticSearch indexes.
   * @param jobs Seq of clinical jobs (participant_centric, study_centric, ...) or variants (variant_centric, gene_centric, ...)
   * @param studies Seq of studies, only required for clinical jobs
   *
   * Assume the index of the format:
   *  Clinical jobs: "[JOB_TYPE]_[STUDY]_..."
   *  Variant jobs: "[JOB_TYPE]_..."
   *
   *  Note: assumed index are fetched from alias, release_id not required
   */
  def generateRegexCurrentAlias(jobs: Seq[String], studies: Option[Seq[String]]): String = {
    val aliasJoin = jobs.mkString("|")

    studies match {
      case Some(s) => s"^($aliasJoin)_(${s.mkString("|")})_.+$$"
      case None => s"^($aliasJoin)_(.*).+$$"
    }
  }

  /**
   * Will generate the regex to fetch desired ElasticSearch indexes.
   * @param jobs Seq of clinical jobs (participant_centric, study_centric, ...) or variants (variant_centric, gene_centric, ...)
   * @param releaseId Release id of clinical or variant run
   * @param studies Seq of studies, only required for clinical jobs
   *
   * Assume the index of the format:
   *  Clinical jobs: "[JOB_TYPE]_[STUDY]_[RELEASE_ID]"
   *  Variant jobs: "[JOB_TYPE]_..._[RELEASE_ID]"
   */
  def generateRegexDesiredIndex(jobs: Seq[String], releaseId: String, studies: Option[Seq[String]]): String = {
    val aliasJoin = jobs.mkString("|")

    studies match {
      case Some(s) => s"^($aliasJoin)_(${s.mkString("|")})_$releaseId$$"
      case None => s"^($aliasJoin)_(.*)$releaseId$$"
    }

  }

  def retrieveIndexesFromRegex(regex: String, fromQuery: String)(esNodes: String, esPort: String)
                              (implicit esHttpClient: EsHttpClient): Seq[String] = {
    val httpRequest = new HttpGet(s"$esNodes/_cat/$fromQuery?h=index")

    val (body, status) = esHttpClient.executeHttpRequest(httpRequest)

    if(status < 300) {
      val bodyParsed = body.map( _.split("\n").filter(s => s.matches(regex)).toSeq)
      bodyParsed.getOrElse(Nil)
    } else Nil
  }

  def updateAlias(alias: String, currentIndex: String, opType: String)(esNodes: String, esPort: String)
                 (implicit esHttpClient: EsHttpClient): Unit = {
    val query = s"""{\"actions\":[{\"$opType\":{\"index\":\"$currentIndex\",\"alias\":\"$alias\"}}]}"""
    val httpRequest = new HttpPost(s"$esNodes:$esPort/_aliases")

    httpRequest.setEntity(new StringEntity(query))
    httpRequest.addHeader("Content-Type", "application/json")

    esHttpClient.executeHttpRequest(httpRequest)
  }
}
