package bio.ferlab.fhir.etl

import org.apache.spark.sql.expressions.UserDefinedFunction
import org.apache.spark.sql.functions._
import org.apache.spark.sql.{Column, functions}


object Utils {

  val extractSystemUrl = "^(http[s]?:\\/\\/[A-Za-z0-9-.\\/]+)\\/[A-za-z0-9-?.]+[a-z\\/=]{1}$"
  val gen3Host = "data.kidsfirstdrc.org"
  val dcfHost = "api.gdc.cancer.gov"
  val specimenExtract = "^Specimen\\/([A-Za-z0-9]+)$"
  val patientExtract = "^Patient\\/([A-Za-z0-9]+)$"
  val organizationExtract = "^Organization\\/([A-Za-z0-9]+)$"
  val versionExtract = "^study_version:(.+)$"

  case class Coding(id: Option[String], system: Option[String], version: Option[String], code: Option[String],
                    display: Option[String], userSelected: Option[String])

  case class ValueAge(id: Option[String], value: Option[Long], comparator: Option[String], unit: Option[String], system: Option[String], code: Option[String])

  val extractFirstForSystem: (Column, Seq[String]) => Column = (column: Column, system: Seq[String]) => filter(column, c => regexp_extract(c("system"), extractSystemUrl, 1).isin(system: _*))(0)

  def firstNonNull: Column => Column = arr => filter(arr, a => a.isNotNull)(0)

  val extractValueAge: String => UserDefinedFunction = (url: String) =>
    udf(
      (arr: Seq[(Option[String], Option[ValueAge])])
      => arr
        .find(c => c._1.getOrElse("") == url)
        .map{ case (_, Some(valueAge)) => (valueAge.value, valueAge.unit) })


  val extractKeywords: UserDefinedFunction =
    udf(
      (arr: Seq[(Option[String], Seq[Coding], Option[String])])
      => arr.map(_._3))

  val retrieveRepository: Column => Column = url => when(url like s"%$gen3Host%", "gen3")
    .when(url like s"%$dcfHost%", "dcf")
    .otherwise(null)

  val sanitizeFilename: Column => Column = fileName => slice(split(fileName, "/"), -1, 1)(0)

  val age_on_set: (Column, Seq[(Int, Int)]) => Column = (c, intervals) => {
    val (_, lastHigh) = intervals.last
    intervals.foldLeft(when(c > lastHigh, s"$lastHigh+")) { case (column, (low, high)) =>
      column.when(c >= low && c < high, s"$low - $high")
    }
  }
}
