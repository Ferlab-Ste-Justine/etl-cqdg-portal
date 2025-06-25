package bio.ferlab.fhir.etl.centricTypes

import bio.ferlab.datalake.commons.config.{Configuration, DatasetConf}
import bio.ferlab.datalake.spark3.etl.v2.ETL
import bio.ferlab.datalake.spark3.implicits.DatasetConfImplicits.DatasetConfOperations
import org.apache.spark.sql.functions.{col, filter, lit, lower, struct, transform => sparkTransform}
import org.apache.spark.sql.{DataFrame, SparkSession}

import java.time.LocalDateTime

class ProgramCentric(studyIds: List[String])(implicit configuration: Configuration) extends ETL {

  override val mainDestination: DatasetConf = conf.getDataset("es_index_program_centric")
  val normalized_list: DatasetConf = conf.getDataset("normalized_list")

  override def extract(lastRunDateTime: LocalDateTime = minDateTime,
                       currentRunDateTime: LocalDateTime = LocalDateTime.now())(implicit spark: SparkSession): Map[String, DataFrame] = {
    Seq(normalized_list)
      .map(ds => ds.id -> ds.read.where(col("study_id").isin(studyIds: _*))).toMap // Read all lists, as we don't filter by study_id for program centric

  }

  override def transform(data: Map[String, DataFrame],
                         lastRunDateTime: LocalDateTime = minDateTime,
                         currentRunDateTime: LocalDateTime = LocalDateTime.now())(implicit spark: SparkSession): Map[String, DataFrame] = {

    import org.apache.spark.sql.functions.{coalesce, lit, lower}

    val isManager = (contact: org.apache.spark.sql.Column) =>
      lower(coalesce(contact.getField("role_en"), lit(""))).equalTo(lit("manager")) ||
        lower(coalesce(contact.getField("role_fr"), lit(""))).equalTo(lit("gestionnaire"))


    val transformedProgramDf = data(normalized_list.id)
      .dropDuplicates("fhir_id")
      .withColumn("website", col("research_program_related_artifact")("website"))
      .withColumn("citation_statement", col("research_program_related_artifact")("citation_statement"))
      .withColumn("logo_url", col("research_program_related_artifact")("logo_url"))
      .withColumn("partners", sparkTransform(col("research_program_partners"), partner =>  struct(
        partner("name"),
        partner("logo") as "logo_url",
        partner("rank"),
      )))
      .withColumn("research_program_contacts_telecom", sparkTransform(col("research_program_contacts"), contact => struct(
        contact("name"),
        contact("institution"),
        contact("role_en"),
        contact("role_fr"),
        contact("picture_url"),
        //other telecom systems can be added here if needed. for possible values see https://build.fhir.org/valueset-contact-point-system.html
        filter(contact("telecom"), telecom => telecom("system") === "email")(0)("value") as "email",
        filter(contact("telecom"), telecom => telecom("system") === "url")(0)("value") as "website",
      )))
      .withColumn(
        "managers",
        filter(col("research_program_contacts_telecom"), isManager)
      )
      .withColumn(
        "contacts",
        filter(col("research_program_contacts_telecom"), contact => !isManager(contact))
      )
      .drop("research_program_related_artifact", "research_program_partners", "research_program_contacts_telecom",
        "research_program_contacts")

    Map(mainDestination.id -> transformedProgramDf)
  }
}
