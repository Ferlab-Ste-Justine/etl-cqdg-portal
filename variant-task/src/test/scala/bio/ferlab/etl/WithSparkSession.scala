package bio.ferlab.etl

import org.apache.hadoop.security.UserGroupInformation
import org.apache.spark.sql.SparkSession

trait WithSparkSession {
  UserGroupInformation.setLoginUser(UserGroupInformation.createRemoteUser("test"))

  implicit lazy val spark: SparkSession = SparkSession.builder()
    .config("spark.ui.enabled", value = false)
    .config("spark.fhir.server.url", "http://localhost:8080")
    .master("local")
    .getOrCreate()

  spark.sparkContext.setLogLevel("ERROR")

}
