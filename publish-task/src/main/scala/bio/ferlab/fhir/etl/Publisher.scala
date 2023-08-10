package bio.ferlab.fhir.etl

import bio.ferlab.datalake.spark3.elasticsearch.ElasticSearchClient
import org.apache.hadoop.shaded.org.apache.http.client.methods.HttpGet
import org.apache.hadoop.shaded.org.apache.http.util.EntityUtils
import org.json4s._
import org.json4s.jackson.JsonMethods._

object Publisher {

  def retrievePreviousIndex(alias: String, studyId: String)(implicit esClient: ElasticSearchClient): Option[String] = {
    val response = esClient.getAliasIndices(alias)
    response.find(s => s.startsWith(s"${alias}_${studyId.toLowerCase}"))
  }

  def publish(alias: String,
              currentIndex: String,
              previousIndex: Option[String] = None)(implicit esClient: ElasticSearchClient): Unit = {
    esClient.setAlias(add = List(currentIndex), remove = previousIndex.toList, alias)
  }
}
