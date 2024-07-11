package bio.ferlab.fhir.etl

import bio.ferlab.fhir.etl.PublishTask.{esNodes, esPort}
import org.apache.http.client.methods.{HttpGet, HttpPost}
import org.apache.http.entity.StringEntity

object Publisher {

  private val patternIndex = """^([a-z0-9_\-.]*)\s+([a-z0-9_\-.]*).*$""".r

  def retrievePreviousIndices(jobs: Seq[String], studies: Seq[String])(implicit esHttpClient: EsHttpClient): Seq[String] = {
    val httpRequest = new HttpGet(s"$esNodes:$esPort/_cat/aliases")

    val (body, status) = esHttpClient.executeHttpRequest(httpRequest)

    if(status < 300) {
      val bodyParsed = body.map( b => {
        val aliases = b.split("\n").flatMap(s => {
          val test = patternIndex.findAllIn(s)
          if(test.hasNext){
            Some(test.group(1) -> test.group(2))
          } else None
        }).toSeq

        aliases
          .filter{ case(a, i) => jobs.contains(a) & studies.exists(study => i.containsSlice(study)) }.map{ case(_, index) => index }
      })

      bodyParsed.getOrElse(Nil)
    } else Nil
  }

  def updateAlias(alias: String, currentIndex: String, opType: String)(implicit esHttpClient: EsHttpClient): Unit = {
    val query = s"""{\"actions\":[{\"$opType\":{\"index\":\"$currentIndex\",\"alias\":\"$alias\"}}]}"""

    val httpRequest = new HttpPost(s"$esNodes/_aliases")
    httpRequest.setEntity(new StringEntity(query))
    httpRequest.addHeader("Content-Type", "application/json")

    esHttpClient.executeHttpRequest(httpRequest)
  }
}
