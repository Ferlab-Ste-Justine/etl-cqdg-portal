package bio.ferlab.fhir.etl

import bio.ferlab.datalake.commons.config.{Configuration, DatasetConf}
import bio.ferlab.datalake.spark3.etl.v2.RawToNormalizedETL
import bio.ferlab.datalake.spark3.implicits.DatasetConfImplicits.DatasetConfOperations
import bio.ferlab.datalake.spark3.transformation.Transformation
import org.apache.spark.sql.functions.col
import org.apache.spark.sql.{DataFrame, SparkSession}

import java.time.LocalDateTime

class ImportRawToNormalizedETL(
    override val source: DatasetConf,
    override val mainDestination: DatasetConf,
    override val transformations: List[Transformation],
    val studyIds: List[String]
)(override implicit val conf: Configuration)
    extends RawToNormalizedETL(source, mainDestination, transformations) {
  override def extract(lastRunDateTime: LocalDateTime, currentRunDateTime: LocalDateTime)(implicit
      spark: SparkSession
  ): Map[String, DataFrame] = {
    log.info(s"extracting: ${source.location}")
    
    // Read with mergeSchema option to handle schema inconsistencies across study partitions
    val df = spark.read
      .format(source.format.sparkFormat)
      .option("mergeSchema", "true")
      .load(source.location)
      .where(col("study_id").isin(studyIds: _*))
    
    Map(source.id -> df)
  }

  override def replaceWhere: Option[String] = Some(s"study_id in (${studyIds.map(s => s"'$s'").mkString(", ")})")
}
