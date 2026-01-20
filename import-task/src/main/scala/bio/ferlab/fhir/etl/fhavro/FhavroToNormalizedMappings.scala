package bio.ferlab.fhir.etl.fhavro

import bio.ferlab.datalake.commons.config.{Configuration, DatasetConf, Format}
import bio.ferlab.datalake.spark3.transformation.{Custom, Transformation}
import bio.ferlab.fhir.etl.transformations.Transformations.extractionMappings
import org.apache.spark.sql.functions.{col, lit, regexp_extract}

import scala.util.matching.Regex

object FhavroToNormalizedMappings {
  val pattern: Regex = "raw_([A-Za-z0-9-_]+)".r

  // using id regex from: https://fhir-ru.github.io/datatypes.html
  val idFromUrlRegex = "^http[s]?:\\/\\/.*\\/([A-Za-z0-9\\-\\.]{1,64})\\/_history"

  def defaultTransformations(): List[Transformation] = {
    List(Custom(_.withColumn("fhir_id", regexp_extract(col("id"), idFromUrlRegex, 1))))
  }

  def mappings()(implicit c: Configuration): List[(DatasetConf, DatasetConf, List[Transformation])] = {
    c.sources
      .filter(s => s.format == Format.AVRO)
      .map(s => {
        val pattern(table) = s.id
        (s, c.getDataset(s"normalized_$table"), defaultTransformations() ++ extractionMappings(table))
      })
  }
}
