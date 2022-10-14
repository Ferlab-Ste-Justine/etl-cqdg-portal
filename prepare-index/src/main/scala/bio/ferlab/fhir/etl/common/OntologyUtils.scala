package bio.ferlab.fhir.etl.common

import bio.ferlab.fhir.etl.common.OntologyUtils.displayTerm
import bio.ferlab.fhir.etl.common.Utils.observableTitleStandard
import org.apache.spark.sql.{Column, DataFrame}
import org.apache.spark.sql.functions._

object OntologyUtils {

  val displayTerm: (Column, Column) => Column = (id, name) => concat(name, lit(" ("), id, lit(")"))
  val transformAncestors: (Column, Column) => Column = (ancestors, age) => transform(ancestors, a =>
    struct(
      displayTerm(a("id"), a("name")) as "name",
      a("parents") as "parents",
      lit(false) as "is_tagged",
      lit(false) as "is_leaf",
      age as "age_at_event_days"
    )
  )

  val transformTaggedTerm: (Column, Column, Column, Column, Column) => Column = (id, name, parents, is_leaf, age) =>
    struct(
      displayTerm(id, name) as "name",
      parents as "parents",
      lit(true) as "is_tagged",
      is_leaf as "is_leaf",
      age as "age_at_event_days"
    )

  val firstCategory: (String, Column) => Column = (category, codes) => filter(codes, code => code("category") === lit(category))(0)("code")

  def addDiseases(df: DataFrame, mondoTerms: DataFrame): DataFrame = {
    val mondoTermsIdName = mondoTerms.select(col("id") as "mondo_term_id", col("name") as "mondo_name")
    df
      //filter out disease with empty code
      .where(size(col("condition_coding")) > 0)
      .withColumn("icd_id_diagnosis", firstCategory("ICD", col("condition_coding")))
      .withColumn("ncit_id_diagnosis", firstCategory("NCIT", col("condition_coding")))
      //Assumption -> age_at_event is in days from birth
      .withColumn("age_at_event_days", col("age_at_event.value"))
      .drop("condition_coding", "release_id")
      .join(mondoTermsIdName, col("mondo_id") === mondoTermsIdName("mondo_term_id"), "left_outer")
      .withColumn("mondo_id_diagnosis", displayTerm(col("mondo_id"), col("mondo_name")))
      .drop("mondo_term_id","mondo_name")
  }

  def generateTaggedPhenotypes (phenotypes: DataFrame, colName: String): DataFrame = {
    phenotypes
      .filter(col("phenotype_id").isNotNull)
      .groupBy("cqdg_participant_id")
      .agg(collect_list(struct(
        col("fhir_id") as "internal_phenotype_id",
        lit(true) as "is_tagged",
        col("phenotype_id"),
        col("is_leaf"),
        col("name"),
        col("parents"),
        col("age_at_event"),
        displayTerm(col("id"), col("name")) as "display_name",
      )) as colName)
  }

  def generatePhenotypeWithAncestors (observedPhenotypes: DataFrame, colName: String): DataFrame = {
    observedPhenotypes
      .withColumn("ancestors_exp", explode(col("ancestors")))
      .withColumn("age_at_event", lit(0))  ///fixme - replace with real value when available
      .withColumn("term", struct(
        col("id"),
        col("name"),
        col("parents"),
        col("is_leaf"),
        lit(true) as "is_tagged",
        displayTerm(col("id"), col("name")) as "display_name",
      ))
      .withColumn("ancestors_with_age", struct(
        col("ancestors_exp.id"),
        col("ancestors_exp.name"),
        col("ancestors_exp.parents"),
        lit(false) as "is_leaf",
        lit(false) as "is_tagged",
        displayTerm(col("ancestors_exp.id"), col("ancestors_exp.name")) as "display_name",
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
        col("all_terms_grouped_exp.id") as "id",
        col("all_terms_grouped_exp.name") as "name",
        col("all_terms_grouped_exp.parents") as "parents",
        col("all_terms_grouped_exp.is_leaf") as "is_leaf",
        col("all_terms_grouped_exp.is_tagged") as "is_tagged",
        col("all_terms_grouped_exp.display_name") as "display_name",
        collect_list("age_at_event") as "age_at_event"
      ) as "phenotype_with_age_grouped")
      .groupBy( "study_id", "cqdg_participant_id")
      .agg(collect_list(col("phenotype_with_age_grouped")) as colName)
  }

  def getTaggedPhenotypes(phenotypesDF: DataFrame, hpoTerms: DataFrame): (DataFrame, DataFrame, DataFrame) = {
    val phenotypesWithTerms = phenotypesDF
      .withColumn("phenotype_id", col("phenotype_HPO_code")("code"))
      .join(hpoTerms, col("phenotype_id") === col("id"), "left_outer")


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
      .join(taggedIcdTerms, col("subject") === col("cqdg_participant_id"), "left_outer")
      .join(taggedMondoTerms, Seq("cqdg_participant_id"), "left_outer")
      .withColumnRenamed("fhir_id", "internal_diagnosis_id")
      .drop("subject", "study_id", "release_id"),
      mondoWithAncestors.drop("study_id"))
  }

  def mapObservableTerms(df: DataFrame, pivotColumn: String)(observableTerms: DataFrame): DataFrame = {
    df
      .join(observableTerms, col(pivotColumn) === col("id"), "left_outer")
      .withColumn("transform_ancestors", when(col("ancestors").isNotNull, transformAncestors(col("ancestors"), col("age_at_event_days"))))
      .withColumn("transform_tagged_observable", transformTaggedTerm(col("id"), col("name"), col("parents"), col("is_leaf"), col("age_at_event_days")))
      .withColumn("observable_with_ancestors", array_union(col("transform_ancestors"), array(col("transform_tagged_observable"))))
      .drop("transform_ancestors", "transform_tagged_observable", "ancestors", "id", "is_leaf", "name", "parents")
  }

  def groupObservableTermsByAge(df: DataFrame, observableTermColName: String): DataFrame = {
    df
      .select(
        "participant_fhir_id",
        s"$observableTermColName.name",
        s"$observableTermColName.parents",
        s"$observableTermColName.is_tagged",
        s"$observableTermColName.is_leaf",
        s"$observableTermColName.age_at_event_days",
      )
      .groupBy("participant_fhir_id", "name", "parents", "is_tagged", "is_leaf")
      .agg(
        col("participant_fhir_id"),
        struct(
          col("name"),
          col("parents"),
          col("is_tagged"),
          col("is_leaf"),
          collect_set(col("age_at_event_days")) as "age_at_event_days"
        ) as observableTermColName,
      )
      .groupBy("participant_fhir_id")
      .agg(
        collect_list(observableTermColName) as observableTermColName
      )
  }
}
