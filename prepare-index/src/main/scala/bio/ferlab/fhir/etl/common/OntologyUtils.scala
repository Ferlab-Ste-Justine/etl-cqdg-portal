package bio.ferlab.fhir.etl.common

import org.apache.spark.sql.functions._
import org.apache.spark.sql.{Column, DataFrame}

object OntologyUtils {

  val displayTerm: (Column, Column) => Column = (id, name) => concat(name, lit(" ("), id, lit(")"))

  def generateTaggedPhenotypes (phenotypes: DataFrame, colName: String): DataFrame = {
    phenotypes
      .filter(col("phenotype_id").isNotNull)
      .groupBy("cqdg_participant_id")
      .agg(collect_list(struct(
        col("fhir_id") as "internal_phenotype_id",
        lit(true) as "is_tagged",
        col("is_leaf"),
        col("parents"),
        col("age_at_event"),
        displayTerm(col("phenotype_id"), col("name")) as "name",
      )) as colName)
  }

  def generatePhenotypeWithAncestors (observedPhenotypes: DataFrame, colName: String): DataFrame = {
    observedPhenotypes
      .withColumn("ancestors_exp", explode(col("ancestors")))
      .withColumn("age_at_event", lit(0))  ///fixme - replace with real value when available
      .withColumn("term", struct(
        col("parents"),
        col("is_leaf"),
        lit(true) as "is_tagged",
        displayTerm(col("phenotype_id"), col("name")) as "name",
      ))
      .withColumn("ancestors_with_age", struct(
        col("ancestors_exp.parents"),
        lit(false) as "is_leaf",
        lit(false) as "is_tagged",
        displayTerm(col("ancestors_exp.id"), col("ancestors_exp.name")) as "name",
      ))
      .groupBy("study_id","cqdg_participant_id", "age_at_event")
      .agg(
        collect_set(col("ancestors_with_age")) as "ancestors_with_age_grouped",
        collect_set(col("term")) as "tagged_terms_grouped",
      )
      .withColumn("all_terms_grouped", concat(col("ancestors_with_age_grouped"), col("tagged_terms_grouped")))
      .drop("ancestors_with_age_grouped", "tagged_terms_grouped")
      .withColumn("all_terms_grouped_exp", explode(col("all_terms_grouped")))
      .groupBy("study_id", "cqdg_participant_id", "all_terms_grouped_exp")
      .agg(struct(
        col("all_terms_grouped_exp.parents") as "parents",
        col("all_terms_grouped_exp.is_leaf") as "is_leaf",
        col("all_terms_grouped_exp.is_tagged") as "is_tagged",
        col("all_terms_grouped_exp.name") as "name",
        collect_list("age_at_event") as "age_at_event"
      ) as "phenotype_with_age_grouped")
      .groupBy( "study_id", "cqdg_participant_id")
      .agg(collect_list(col("phenotype_with_age_grouped")) as colName)
  }

  def getTaggedPhenotypes(phenotypesDF: DataFrame, hpoTerms: DataFrame): (DataFrame, DataFrame, DataFrame) = {
    val phenotypesWithTerms = phenotypesDF
      .withColumn("phenotype_id", col("phenotype_HPO_code")("code"))
      .join(hpoTerms, col("phenotype_id") === col("id"), "left_outer")
      .drop(col("id"))

    val observedPhenotypes = phenotypesWithTerms
      .filter(col("phenotype_observed").equalTo("POS"))
      .withColumn("age_at_event", lit(0))//FIXME - replace with actual value when available

    val nonObservedPhenotypes = phenotypesWithTerms
      .filter(col("phenotype_observed").notEqual("POS") || col("phenotype_observed").isNull)
      .withColumn("age_at_event", lit(0))//FIXME - replace with actual value when available

    val observedPhenotypesWithAncestors = generatePhenotypeWithAncestors(observedPhenotypes, "observed_phenotypes")

    (generateTaggedPhenotypes(observedPhenotypes, "observed_phenotype_tagged"),
      generateTaggedPhenotypes(nonObservedPhenotypes, "non_observed_phenotype_tagged"),
      observedPhenotypesWithAncestors.drop("study_id"))
  }

  def getDiagnosis(diagnosisDf: DataFrame, mondoTerms: DataFrame, icdTerms: DataFrame): (DataFrame, DataFrame) = {
    val mondoWithTerms = diagnosisDf
      .withColumnRenamed("diagnosis_mondo_code", "phenotype_id")
      .join(mondoTerms, col("phenotype_id") === col("id"), "left_outer")
      .withColumn("age_at_event", col("age_at_diagnosis")("value"))
      .withColumnRenamed("subject", "cqdg_participant_id")

    val taggedMondoTerms = generateTaggedPhenotypes(mondoWithTerms, "tagged_mondo")

    val mondoWithAncestors = generatePhenotypeWithAncestors(mondoWithTerms, "mondo")

    val icdSplitId = icdTerms
      .withColumn("id", split(col("id"), "\\|")(0))

    val icdWithTerms = diagnosisDf
      .withColumnRenamed("diagnosis_ICD_code", "phenotype_id")
      .join(icdSplitId, col("phenotype_id") === col("id"), "left_outer")
      .withColumn("age_at_event", col("age_at_diagnosis")("value"))
      .withColumnRenamed("subject", "cqdg_participant_id")

    val taggedIcdTerms = generateTaggedPhenotypes(icdWithTerms, "tagged_icd")

    (diagnosisDf
      .withColumn("age_at_diagnosis", col("age_at_diagnosis")("value"))
      .groupBy("subject", "study_id", "release_id")
      .agg(collect_list(struct(
        col("fhir_id"),
        col("diagnosis_source_text"),
        col("diagnosis_mondo_code"),
        col("diagnosis_ICD_code"),
        col("age_at_diagnosis")
      )) as "diagnoses")
      .join(taggedIcdTerms, col("subject") === col("cqdg_participant_id"), "left_outer")
      .join(taggedMondoTerms, Seq("cqdg_participant_id"), "left_outer")
      .withColumnRenamed("fhir_id", "internal_diagnosis_id")
      .drop("subject", "study_id", "release_id"),
      mondoWithAncestors.drop("study_id"))
  }
}
