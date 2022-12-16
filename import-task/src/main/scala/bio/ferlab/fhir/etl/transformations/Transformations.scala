package bio.ferlab.fhir.etl.transformations

import bio.ferlab.datalake.spark3.transformation.{Custom, Drop, Transformation}
import bio.ferlab.fhir.etl.Utils._
import bio.ferlab.fhir.etl._
import org.apache.spark.sql.Column
import org.apache.spark.sql.functions.{col, _}

object Transformations {

  val patientMappings: List[Transformation] = List(
    Custom(_
      .select("fhir_id", "study_id", "release_id", "identifier", "extension", "gender", "deceasedBoolean")
      .withColumn("age_at_recruitment", firstNonNull(transform(
        filter(col("extension"),col => col("url") === AGE_AT_RECRUITMENT_S_D)("valueAge"),
        col => col("value")
      )))
      .withColumn("ethnicity",
        firstNonNull(firstNonNull(filter(col("extension"),col => col("url") === ETHNICITY_S_D)("valueCodeableConcept"))("coding"))("code")
      )
      .withColumn("submitter_participant_id", firstNonNull(filter(col("identifier"), col => col("use") === "secondary"))("value"))
      .withColumnRenamed("deceasedBoolean", "vital_status")
      .withColumn("age_of_death", filter(col("extension"), col => col("url") === AGE_OF_DEATH_S_D)(0)("valueAge")("value"))
    ),
    Drop("identifier", "extension")
  )

  val taskMappings: List[Transformation] = List(
    Custom(_
      .select("fhir_id", "study_id", "release_id", "output", "code", "for", "owner", "extension")
      .withColumn("bio_informatic_analysis", filter(col("code")("coding"), col => col("system") === TASK_BIO_INFO)(0)("code"))
      .withColumn("seq_exp", filter(col("extension"), col => col("url") === SEQUENCING_EXPERIMENT_S_D)(0))
      .withColumn("workflow", filter(col("extension"), col => col("url") === WORKFLOW_S_D)(0))
      .withColumn("labAliquotID", filter(col("seq_exp")("extension"), col => col("url") === "labAliquotId")(0)("valueString"))
      .withColumn("run_name", filter(col("seq_exp")("extension"), col => col("url") === "runName")(0)("valueString"))
      .withColumn("read_length", filter(col("seq_exp")("extension"), col => col("url") === "readLength")(0)("valueString"))
      .withColumn("is_paired_end", filter(col("seq_exp")("extension"), col => col("url") === "isPairedEnd")(0)("valueBoolean"))
      .withColumn("run_alias", filter(col("seq_exp")("extension"), col => col("url") === "runAlias")(0)("valueString"))
      .withColumn("run_date", filter(col("seq_exp")("extension"), col => col("url") === "runDate")(0)("valueDateTime"))
      .withColumn("capture_kit", filter(col("seq_exp")("extension"), col => col("url") === "captureKit")(0)("valueString"))
      .withColumn("platform", filter(col("seq_exp")("extension"), col => col("url") === "platform")(0)("valueString"))
      .withColumn("experimental_strategy", transform(
        filter(col("seq_exp")("extension"), col => col("url") === "experimentalStrategy") , col => col("valueCoding")("code")
      ))
      .withColumn("sequencer_id", filter(col("seq_exp")("extension"), col => col("url") === "sequencerId")(0)("valueString"))
      .withColumn("workflow_name", filter(col("workflow")("extension"), col => col("url") === "workflowName")(0)("valueString"))
      .withColumn("workflow_version", filter(col("workflow")("extension"), col => col("url") === "workflowVersion")(0)("valueString"))
      .withColumn("genome_build", filter(col("workflow")("extension"), col => col("url") === "genomeBuild")(0)("valueCoding")("code"))
      .withColumn("_for", regexp_extract(col("for")("reference"), patientExtract, 1))
      .withColumn("owner", regexp_extract(col("owner")("reference"), organizationExtract, 1))
      .withColumn("clean_output", transform(col("output"), col => struct(col("type")("coding")(0) as "code", col("valueReference") as "value")))
      .withColumn("alir", filter(col("clean_output"), col => col("code")("code") === "Aligned Reads")(0)("value")("reference"))
      .withColumn("snv", filter(col("clean_output"), col => col("code")("code") === "SNV")(0)("value")("reference"))
      .withColumn("gcnv", filter(col("clean_output"), col => col("code")("code") === "Germline CNV")(0)("value")("reference"))
      .withColumn("gsv", filter(col("clean_output"), col => col("code")("code") === "Germline Structural Variant")(0)("value")("reference"))
      .withColumn("ssup", filter(col("clean_output"), col => col("code")("code") === "Sequencing Data Supplement")(0)("value")("reference"))
    ),
    Drop("seq_exp", "extension", "workflow", "code", "output", "clean_output")
  )

  val biospecimenMappings: List[Transformation] = List(
    Custom { _
      .select("fhir_id", "extension", "identifier", "subject", "study_id", "release_id", "type")
      .where(size(col("parent")) === 0)
      .withColumn("subject", regexp_extract(col("subject")("reference"), patientExtract, 1))
      .withColumn("biospecimen_tissue_source", col("type")("coding")(0)("code"))
      .withColumn("age_biospecimen_collection", extractValueAge(AGE_BIO_COLLECTION_S_D)(col("extension")).cast("struct<value:long,unit:string>"))
      .withColumn("submitter_biospecimen_id", firstNonNull(filter(col("identifier"), col => col("use") === "secondary")("value")))
    },
    Drop("type", "extension", "identifier")
  )

  val sampleRegistrationMappings: List[Transformation] = List(
    Custom { _
      .select("identifier", "type", "subject", "parent", "study_id", "release_id", "fhir_id")
      .where(size(col("parent")) > 0)
      .withColumn("sample_type",
        transform(col("type")("coding"), col => struct(col("system") as "system", col("code") as "code"))(0))
      .withColumn("submitter_sample_id", firstNonNull(filter(col("identifier"), col => col("use") === "secondary")("value")))
      .withColumn("subject",  regexp_extract(col("subject")("reference"), patientExtract, 1))
      .withColumn("parent", firstNonNull(transform(col("parent"),  col => regexp_extract(col("reference"), specimenExtract, 1))))
    },
    Drop("identifier", "type")
  )

  val observationCauseOfDeathMappings: List[Transformation] = List(
    Custom(_
      .select("study_id", "release_id", "fhir_id", "code", "subject")
      .filter(array_contains(col("code")("coding")("system"),"https://fhir.cqdg.ferlab.bio/CodeSystem/cause-of-death"))
      .withColumn("submitter_participant_ids", regexp_extract(col("subject")("reference"), patientExtract, 1))
      .withColumn("cause_of_death", col("code")("coding")("code")(0))
    ),
    Drop("code", "subject")
  )

  val observationDiseaseStatus: List[Transformation] = List(
    Custom(_
      .select("study_id", "release_id", "fhir_id", "code", "subject", "valueCodeableConcept")
      .filter(array_contains(col("code")("coding")("code"),"Disease Status"))
      .withColumn("subject", regexp_extract(col("subject")("reference"), patientExtract, 1))
      .withColumn("disease_status", col("valueCodeableConcept")("coding")("code")(0))
    ),
    Drop("code", "valueCodeableConcept")
  )

  val observationTumorNormalDesignation: List[Transformation] = List(
    Custom(_
      .select("study_id", "release_id", "fhir_id", "code", "subject", "valueCodeableConcept")
      .filter(array_contains(col("code")("coding")("code"),"Tumor Normal Designation"))
      .withColumn("subject", regexp_extract(col("subject")("reference"), patientExtract, 1))
      .withColumn("tumor_normal_designation", col("valueCodeableConcept")("coding")("code")(0))
    ),
    Drop("code", "valueCodeableConcept")
  )

  val observationFamilyRelationshipMappings: List[Transformation] = List(
    Custom(_
      .select("study_id", "release_id", "fhir_id", "code", "subject", "focus", "valueCodeableConcept", "category")
      .withColumnRenamed("fhir_id", "internal_family_relationship_id")
      .where(col("code")("coding")(0)("code") === "Family Relationship")
      .withColumn("submitter_participant_id",  regexp_extract(col("subject")("reference"), patientExtract, 1))
      .withColumn("focus_participant_id", firstNonNull(transform(col("focus"),  col => regexp_extract(col("reference"), patientExtract, 1))))
      .withColumn("relationship_to_proband", firstNonNull(transform(col("valueCodeableConcept")("coding"), col => col("code"))))
      .withColumn("category", col("category")(0)("coding")(0)("code"))
    ),
    Drop("code", "valueCodeableConcept", "subject", "focus")
  )

  val conditionDiagnosisMappings: List[Transformation] = List(
    Custom(_
      .select("identifier", "code", "subject", "onsetAge", "study_id", "release_id", "fhir_id")
      .withColumn("diagnosis_source_text", col("code")("text"))
      .withColumn("diagnosis_mondo_code",
        firstNonNull(filter(col("code")("coding"), col => col("system").equalTo("http://purl.obolibrary.org/obo/mondo.owl"))("code")))
      .withColumn("diagnosis_ICD_code", firstNonNull(filter(col("code")("coding"), col => col("system").isNull)("code")))
      .withColumn("subject", regexp_extract(col("subject")("reference"), patientExtract, 1))
      .withColumn("age_at_diagnosis", struct(col("onsetAge")("value") as "value", col("onsetAge")("unit") as "unit"))
    ),
    Drop("identifier", "code", "onsetAge")
  )

  val observationPhenotypeMappings: List[Transformation] = List(
    Custom(_
      .select("study_id", "release_id", "fhir_id", "code", "valueCodeableConcept", "subject", "interpretation", "extension")
      .where(col("code")("coding")(0)("code") === "Phenotype")
      .withColumn("age_at_phenotype", firstNonNull(transform(
        filter(col("extension"),col => col("url") === AGE_AT_PHENOTYPE_S_D)("valueAge"),
        col => col("value")
      )))
      .withColumn("phenotype_source_text", col("code")("text"))
      .withColumn("phenotype_HPO_code",
         firstNonNull(transform(col("valueCodeableConcept")("coding"), col => struct(col("system") as "system", col("code") as "code")))
      )
      .withColumn("cqdg_participant_id", regexp_extract(col("subject")("reference"), patientExtract, 1))
      .withColumn("phenotype_observed", col("interpretation")(0)("coding")(0)("code"))
    ),
    Drop("code", "valueCodeableConcept", "subject", "interpretation", "extension")
  )


  val researchstudyMappings: List[Transformation] = List(
    Custom(_
      .select("fhir_id", "keyword", "release_id", "study_id", "description", "contact", "category", "status", "title", "extension", "meta", "identifier")
      .withColumn("keyword", extractKeywords(col("keyword")))
      .withColumn(
        "contact", transform(col("contact"), col => struct(col("telecom")(0)("system") as "type", col("telecom")(0)("value") as "value"))(0)
      )
      .withColumn("domain", col("category")("coding")(0)("code"))
      .withColumn("study_code", firstNonNull(filter(col("identifier"), col => col("use") === "secondary"))("value"))
      .withColumn("access_limitations", filter(col("extension"), col => col("url") === ACCESS_LIMITATIONS_S_D)(0)("valueCodeableConcept")("coding")("code"))
      .withColumn("access_requirements", filter(col("extension"), col => col("url") === ACCESS_REQUIREMENTS_S_D)(0)("valueCodeableConcept")("coding")("code"))
      .withColumn("population", filter(col("extension"), col => col("url") === POPULATION_S_D)(0)("valueCoding")("code"))
      .withColumn("study_version", regexp_extract(filter(col("meta")("tag"), col => col("code").contains("study_version"))(0)("code"), versionExtract, 1))
    ),
    Drop("extension", "category", "meta", "identifier")
  )

  val documentreferenceMappings: List[Transformation] = List(
    Custom { input =>
      val columns = Array("id", "type", "category", "subject", "content", "context", "study_id", "release_id", "fhir_id")
      val df = input
        .select(columns.head, columns.tail: _*)
        .withColumn("participant_id", regexp_extract(col("subject")("reference"), patientExtract, 1))
        .withColumn("biospecimen_reference", regexp_extract(col("context")("related")(0)("reference"), specimenExtract, 1))
        .withColumn("data_type", filter(col("type")("coding"), col => col("system") === DOCUMENT_DATA_TYPE)(0)("code"))
        .withColumn("data_category", filter(col("category")(0)("coding"), col => col("system") === DOCUMENT_DATA_CATEGORY)(0)("code"))
        .withColumn("content_exp", explode(col("content")))
        .withColumn("file_size", firstNonNull(filter(col("content_exp")("attachment")("extension"), col => col("url") === DOCUMENT_SIZE_S_D))("valueDecimal"))
        .withColumn("ferload_url", col("content_exp")("attachment")("url"))
        .withColumn("file_hash", col("content_exp")("attachment")("hash"))
        .withColumn("file_name", col("content_exp")("attachment")("title"))
        .withColumn("file_format", col("content_exp")("format")("code"))
        .groupBy(columns.head, columns.tail ++ Array("participant_id", "biospecimen_reference", "data_type", "data_category"): _*)
        .agg(
          collect_list(
            struct(
              col("file_name"),
              col("file_format"),
              col("file_size"),
              col("ferload_url"),
              col("file_hash"),
            )
          ) as "files",
        )
      df
    },
    Drop("id", "type", "category", "subject", "content", "context")
  )

  val groupMappings: List[Transformation] = List(
    Custom(_
      .select("study_id", "release_id", "fhir_id", "code", "member", "identifier")
      .withColumnRenamed("fhir_id", "internal_family_id")
      .withColumn("family_type", firstNonNull(transform(col("code")("coding"), col => col("code"))))
      .withColumn("family_members", transform(col("member"), col => regexp_extract(col("entity")("reference"), patientExtract, 1)))
      .withColumn("submitter_family_id", col("identifier")(0)("value"))
    ),
    Drop("code", "member", "identifier")
  )

  val extractionMappings: Map[String, List[Transformation]] = Map(
    "patient" -> patientMappings,
    "task" -> taskMappings,
    "biospecimen" -> biospecimenMappings,
    "sample_registration" -> sampleRegistrationMappings,
    "research_study" -> researchstudyMappings,
    "group" -> groupMappings,
    "document_reference" -> documentreferenceMappings,
    "diagnosis" -> conditionDiagnosisMappings,
    "phenotype" -> observationPhenotypeMappings,
    "family_relationship" -> observationFamilyRelationshipMappings,
    "cause_of_death" -> observationCauseOfDeathMappings,
    "disease_status" -> observationDiseaseStatus,
    "tumor_normal_designation" -> observationTumorNormalDesignation,
  )

}
