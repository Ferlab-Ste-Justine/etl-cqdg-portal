package bio.ferlab.fhir.etl

import org.apache.spark.sql.expressions.UserDefinedFunction
import org.apache.spark.sql.functions._
import org.apache.spark.sql.{Column, functions}


object Utils {

  val actCodeR = "^phs[0-9.a-z]+"
  val extractSystemUrl = "^(http[s]?:\\/\\/[A-Za-z0-9-.\\/]+)\\/[A-za-z0-9-?.]+[a-z\\/=]{1}$"
  val gen3Host = "data.kidsfirstdrc.org"
  val dcfHost = "api.gdc.cancer.gov"
  val patternUrnUniqueIdStudy = "[A-Z][a-z]+-(SD_[0-9A-Za-z]+)-([A-Z]{2}_[0-9A-Za-z]+)"
  val documentReferenceExtract = "^DocumentReference\\/([0-9]+)$"
  val specimenExtract = "^Specimen\\/([A-Za-z0-9]+)$"
  val patientExtract = "^Patient\\/([A-Za-z0-9]+)$"
  val phenotypeExtract = "^[A-Z]{2,}.[0-9]+$"

  case class Coding(id: Option[String], system: Option[String], version: Option[String], code: Option[String],
                    display: Option[String], userSelected: Option[String])

  case class ValueCodeableConcept(coding: Seq[Coding])

  case class ValueAge(id: Option[String], value: Option[Long], comparator: Option[String], unit: Option[String], system: Option[String], code: Option[String])


  val extractAclFromList: UserDefinedFunction =
    udf((arr: Seq[String], studyId: String) => arr.filter(e => e != null && ((e matches actCodeR) || (e == studyId))))

  val extractReferencesId: Column => Column = (column: Column) => functions.transform(column, extractReferenceId)

  val extractReferenceId: Column => Column = (column: Column) => functions.split(column, "/")(1)

  val extractStudyId: () => Column = () => regexp_extract(extractFirstForSystem(col("identifier"), Seq(URN_UNIQUE_ID))("value"), patternUrnUniqueIdStudy, 1)

  val extractFirstForSystem: (Column, Seq[String]) => Column = (column: Column, system: Seq[String]) => filter(column, c => regexp_extract(c("system"), extractSystemUrl, 1).isin(system: _*))(0)

  val extractDocUrl: Column => Column = artifacts => filter(artifacts, c => c("type") === "documentation")(0)

  val firstSystemEquals: (Column, String) => Column = (column: Column, system: String) => filter(column, c => c("system") === system)(0)

  val extractOfficial: Column => Column = (identifiers: Column) => coalesce(filter(identifiers, identifier => identifier("use") === "official")(0)("value"), identifiers(0)("value"))


  def firstNonNull: Column => Column = arr => filter(arr, a => a.isNotNull)(0)

  val extractHashes: UserDefinedFunction =
    udf(
      (arr: Seq[(Option[String], Seq[(Option[String], Option[String], Option[String], Option[String], Option[String], Option[Boolean])], Option[String])])
      => arr.map(r => r._2.head._5 -> r._3).toMap)

  val extractValueAge: String => UserDefinedFunction = (url: String) =>
    udf(
      (arr: Seq[(Option[String], Option[ValueAge])])
      => arr
        .find(c => c._1.getOrElse("") == url)
        .map{ case (_, Some(valueAge)) => (valueAge.value, valueAge.unit) })


  val extactSubmitterParticipantID: UserDefinedFunction =
    udf(
      (arr: Seq[(Option[String], Option[ValueAge])])
      => arr
        .find(c => c._1.getOrElse("") == "https://fhir.cqdg.ferlab.bio/StructureDefinition/Specimen/ageBiospecimenCollection")
        .map{ case (_, Some(valueAge)) => (valueAge.value, valueAge.unit) })

  val extractKeywords: UserDefinedFunction =
    udf(
      (arr: Seq[(Option[String], Seq[Coding], Option[String])])
      => arr.map(_._3))

  val extractValueCodeableConcept: String => UserDefinedFunction = (filter: String) =>
    udf((arr: Seq[(Option[String], ValueCodeableConcept)]) =>
      arr.filter(e => e._1.getOrElse("") == filter).flatMap(e => e._2.coding.map(c => (c.code, c.system)))
    )

  val retrieveIsHarmonized: Column => Column = url => url.isNotNull && (url like "harmonized-data")

  val retrieveRepository: Column => Column = url => when(url like s"%$gen3Host%", "gen3")
    .when(url like s"%$dcfHost%", "dcf")
    .otherwise(null)

  val retrieveSize: UserDefinedFunction = udf((d: Option[String]) => d.map(BigInt(_).toLong))

  val extractStudyVersion: UserDefinedFunction = udf((s: Option[String]) => s.map(_.split('.').tail.mkString(".")))

  val extractStudyExternalId: UserDefinedFunction = udf((s: Option[String]) => s.map(_.split('.').head))

  val sanitizeFilename: Column => Column = fileName => slice(split(fileName, "/"), -1, 1)(0)

  val age_on_set: (Column, Seq[(Int, Int)]) => Column = (c, intervals) => {
    val (_, lastHigh) = intervals.last
    intervals.foldLeft(when(c > lastHigh, s"$lastHigh+")) { case (column, (low, high)) =>
      column.when(c >= low && c < high, s"$low - $high")
    }
  }

  val upperFirstLetter: Column => Column = c => concat(upper(substring(c, 1, 1)), lower(substring(c, 2, 10000)))

  val ignoredOmbCategoryCodes = Seq("UNK", "NAVU", "NI")

  val ombCategory: Column => Column = c => when(c("code").isin(ignoredOmbCategoryCodes: _*), lit(null)).otherwise(c("display"))
}
