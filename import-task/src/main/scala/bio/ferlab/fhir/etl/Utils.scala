package bio.ferlab.fhir.etl

import org.apache.spark.sql.Column
import org.apache.spark.sql.expressions.UserDefinedFunction
import org.apache.spark.sql.functions._


object Utils {

  val specimenExtract = "^Specimen\\/([A-Za-z0-9]+)$"
  val patientExtract = "^Patient\\/([A-Za-z0-9]+)$"
  val organizationExtract = "^Organization\\/([A-Za-z0-9]+)$"
  val versionExtract = "^study_version:(.+)$"
  val datasetExtract = "^dataset:(.+)$"

  case class Coding(id: Option[String], system: Option[String], version: Option[String], code: Option[String],
                    display: Option[String], userSelected: Option[String])

  def firstNonNull: Column => Column = arr => filter(arr, a => a.isNotNull)(0)
  def extractDisplay: Column => Column = col => when(isnull(col("coding")(0)("display")), col("coding")(0)("code")).otherwise(col("coding")(0)("display"))
  val retrieveSize: UserDefinedFunction = udf((d: Option[String]) => d.map(BigInt(_).toLong))

  val extractKeywords: UserDefinedFunction =
    udf(
      (arr: Seq[(Option[String], Seq[Coding], Option[String])])
      => arr.map(_._3))

  val ageFromExtension : (Column, String) => Column = (extension, url) => transformAgeSortable(firstNonNull(transform(
    filter(extension, col => col("url") === url)("valueCodeableConcept"),
    extractDisplay
  )))

  // see issue CQDG-490
  val transformAgeSortable: Column => Column = age => when(age contains "Antenatal", "A-antenatal")
      .when(age like "Congenital%", "B-congenital")
      .when(age like "Neonatal%", "C-neonatal")
      .when(age like "Infantile%", "D-infantile")
      .when(age like "Childhood%", "E-childhood")
      .when(age like "Juvenile%", "F-juvenile")
      .when(age like "Young Adult%", "G-young adult")
      .when(age like "Middle Age%", "H-middle age")
      .when(age like "Senior%", "I-senior")
      .otherwise(age)
}
