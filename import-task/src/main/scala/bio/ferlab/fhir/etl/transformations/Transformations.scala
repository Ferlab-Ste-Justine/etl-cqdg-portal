package bio.ferlab.fhir.etl.transformations

import bio.ferlab.datalake.spark3.transformation.{Custom, Drop, Transformation}
import bio.ferlab.fhir.etl.Utils._
import bio.ferlab.fhir.etl._
import org.apache.spark.sql.Column
import org.apache.spark.sql.functions.{col, _}

object Transformations {

  val patientMappings: List[Transformation] = List(
    Custom(_
      .select("fhir_id", "study_id", "identifier", "extension", "gender", "deceasedBoolean", "meta")
      .withColumn("age_at_recruitment", ageFromExtension(col("extension"), AGE_AT_RECRUITMENT_S_D))
      .withColumn("ethnicity",
        firstNonNull(firstNonNull(filter(col("extension"),col => col("url") === ETHNICITY_S_D)("valueCodeableConcept"))("coding"))("code")
      )
      .withColumn("submitter_participant_id", firstNonNull(filter(col("identifier"), col => col("use") === "secondary"))("value"))
      .withColumn("vital_status", when(col("deceasedBoolean"), lit("Deceased"))
          .when(!col("deceasedBoolean"), lit("Alive"))
          .otherwise(lit("Unknown"))
      )
      .withColumn("age_of_death", ageFromExtension(col("extension"), AGE_OF_DEATH_S_D))
      .withColumnRenamed("gender", "sex")
      .withColumn("security", filter(col("meta")("security"), col => col("system") === SYSTEM_CONFIDENTIALITY)(0)("code"))
    ),
    Drop("identifier", "extension", "meta", "gender")
  )

  val taskMappings: List[Transformation] = List(
    Custom(_
      .select("fhir_id", "study_id", "output", "code", "for", "owner", "extension")
      .withColumn("bio_informatic_analysis", filter(col("code")("coding"), col => col("system") === TASK_BIO_INFO)(0)("display"))
      .withColumn("seq_exp", filter(col("extension"), col => col("url") === SEQUENCING_EXPERIMENT_S_D)(0))
      .withColumn("workflow", filter(col("extension"), col => col("url") === WORKFLOW_S_D)(0))
      .withColumn("labAliquotID", filter(col("seq_exp")("extension"), col => col("url") === "labAliquotId")(0)("valueString"))
      .withColumn("ldm_sample_id", filter(col("seq_exp")("extension"), col => col("url") === "ldmSampleId")(0)("valueString"))
      .withColumn("run_name", filter(col("seq_exp")("extension"), col => col("url") === "runName")(0)("valueString"))
      .withColumn("read_length", split(filter(col("seq_exp")("extension"), col => col("url") === "readLength")(0)("valueString"), ",")(0))
      .withColumn("is_paired_end", filter(col("seq_exp")("extension"), col => col("url") === "isPairedEnd")(0)("valueBoolean"))
      .withColumn("run_alias", filter(col("seq_exp")("extension"), col => col("url") === "runAlias")(0)("valueString"))
      // date extracted from fhir are one day before, Likely due to improper time zone setup on the fhir server. This should be fixed
      // and date_add function removed...
      .withColumn("run_date", date_add(date_from_unix_date(filter(col("seq_exp")("extension"), col => col("url") === "runDate")(0)("valueDate")), 1))
      .withColumn("capture_kit", filter(col("seq_exp")("extension"), col => col("url") === "captureKit")(0)("valueString"))
      .withColumn("platform", filter(col("seq_exp")("extension"), col => col("url") === "platform")(0)("valueString"))
      .withColumn("experimental_strategy", filter(col("seq_exp")("extension"), col => col("url") === "experimentalStrategy")(0)("valueCoding")("code"))
      .withColumn("sequencer_id", filter(col("seq_exp")("extension"), col => col("url") === "sequencerId")(0)("valueString"))
      .withColumn("workflow_name", filter(col("workflow")("extension"), col => col("url") === "workflowName")(0)("valueString"))
      .withColumn("workflow_version", filter(col("workflow")("extension"), col => col("url") === "workflowVersion")(0)("valueString"))
      .withColumn("genome_build", filter(col("workflow")("extension"), col => col("url") === "genomeBuild")(0)("valueCoding")("code"))
      .withColumn("_for", regexp_extract(col("for")("reference"), patientExtract, 1))
      .withColumn("owner", regexp_extract(col("owner")("reference"), organizationExtract, 1))
      .withColumn("clean_output", transform(col("output"), col => struct(col("type")("coding")(0) as "code", col("valueReference") as "value")))
      .withColumn("alir", filter(col("clean_output"), col => col("code")("code") === "Aligned-reads")(0)("value")("reference"))
      .withColumn("snv", filter(col("clean_output"), col => col("code")("code") === "SNV")(0)("value")("reference"))
      .withColumn("gcnv", filter(col("clean_output"), col => col("code")("code") === "Germline-CNV")(0)("value")("reference"))
      .withColumn("gsv", filter(col("clean_output"), col => col("code")("code") === "Germline-structural-variant")(0)("value")("reference"))
      .withColumn("ssup", filter(col("clean_output"), col => col("code")("code") === "Sequencing-data-supplement")(0)("value")("reference"))
    ),
    Drop("seq_exp", "extension", "workflow", "code", "output", "clean_output", "for")
  )

  val biospecimenMappings: List[Transformation] = List(
    Custom { _
      .select("fhir_id", "extension", "identifier", "subject", "study_id", "type", "meta", "parent")
      .where(size(col("parent")) === 0)
      .withColumn("subject", regexp_extract(col("subject")("reference"), patientExtract, 1))
      .withColumn("biospecimen_tissue_source",
        transform(col("type")("coding"), col => struct(col("system") as "system", col("code") as "code", col("display") as "display"))(0))
      .withColumn("age_biospecimen_collection", ageFromExtension(col("extension"), AGE_AT_EVENT_S_D))
      .withColumn("submitter_biospecimen_id", firstNonNull(filter(col("identifier"), col => col("use") === "secondary")("value")))
      .withColumn("security", filter(col("meta")("security"), col => col("system") === SYSTEM_CONFIDENTIALITY)(0)("code"))
    },
    Drop("type", "extension", "identifier", "meta", "parent")
  )

  val sampleRegistrationMappings: List[Transformation] = List(
    Custom { _
      .select("identifier", "type", "subject", "parent", "study_id", "fhir_id")
      .where(size(col("parent")) > 0)
      .withColumn("sample_type",
        transform(col("type")("coding"), col => struct(col("system") as "system", col("code") as "code", col("display") as "display"))(0))
      .withColumn("submitter_sample_id", firstNonNull(filter(col("identifier"), col => col("use") === "secondary")("value")))
      .withColumn("subject",  regexp_extract(col("subject")("reference"), patientExtract, 1))
      .withColumn("parent", firstNonNull(transform(col("parent"),  col => regexp_extract(col("reference"), specimenExtract, 1))))
    },
    Drop("identifier", "type")
  )

  val observationCauseOfDeathMappings: List[Transformation] = List(
    Custom(_
      .select("study_id", "fhir_id", "code", "subject")
      .filter(array_contains(col("code")("coding")("system"),"https://fhir.cqdg.ferlab.bio/CodeSystem/cause-of-death"))
      .withColumn("submitter_participant_ids", regexp_extract(col("subject")("reference"), patientExtract, 1))
      .withColumn("cause_of_death", col("code")("coding")("code")(0))
    ),
    Drop("code", "subject")
  )

  val observationDiseaseStatus: List[Transformation] = List(
    Custom(_
      .select("study_id", "fhir_id", "code", "subject", "valueCodeableConcept")
      .filter(array_contains(col("code")("coding")("code"),"Disease Status"))
      .withColumn("subject", regexp_extract(col("subject")("reference"), patientExtract, 1))
      .withColumn("disease_status", col("valueCodeableConcept")("coding")("code")(0))
    ),
    Drop("code", "valueCodeableConcept")
  )

  val observationTumorNormalDesignation: List[Transformation] = List(
    Custom(_
      .select("study_id", "fhir_id", "code", "subject", "valueCodeableConcept")
      .filter(array_contains(col("code")("coding")("code"),"Tumor Normal Designation"))
      .withColumn("subject", regexp_extract(col("subject")("reference"), patientExtract, 1))
      .withColumn("tumor_normal_designation", col("valueCodeableConcept")("coding")("code")(0))
    ),
    Drop("code", "valueCodeableConcept")
  )

  val observationFamilyRelationshipMappings: List[Transformation] = List(
    Custom(_
      .select("study_id", "fhir_id", "code", "subject", "focus", "valueCodeableConcept", "category")
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
      .select("identifier", "code", "subject", "study_id", "fhir_id", "extension")
      .withColumn("diagnosis_source_text", col("code")("text"))
      .withColumn("diagnosis_mondo_code",
        firstNonNull(filter(col("code")("coding"), col => col("system").equalTo(SYSTEM_MONDO))("code")))
      .withColumn("diagnosis_ICD_code",
        firstNonNull(filter(col("code")("coding"), col => col("system").equalTo(SYSTEM_ICD))("code")))
      .withColumn("subject", regexp_extract(col("subject")("reference"), patientExtract, 1))
      .withColumn("age_at_diagnosis", ageFromExtension(col("extension"), AGE_AT_EVENT_S_D))
    ),
    Drop("identifier", "code", "extension")
  )

  val observationPhenotypeMappings: List[Transformation] = List(
    Custom(_
      .select("study_id", "fhir_id", "code", "valueCodeableConcept", "subject", "interpretation", "extension")
      .where(col("code")("coding")(0)("code") === "Phenotype")
      .withColumn("age_at_phenotype", ageFromExtension(col("extension"), AGE_AT_EVENT_S_D))
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
      .select("fhir_id", "keyword", "study_id", "description", "contact", "category", "status", "title", "extension", "meta", "identifier", "relatedArtifact")
      .withColumn("keyword", extractKeywords(col("keyword")))
      .withColumn("telecom", firstNonNull(transform(col("contact"), col => col("telecom")(0))))
      .withColumn("access_authority", struct(col("telecom")("system") as "type", col("telecom")("value")))
      .withColumn("contact_names", transform(filter(col("contact"), col => col("name").isNotNull), col => col("name")))
      .withColumn("contact_extensions", flatten(col("contact")("extension")))
      .withColumn("contact_institutions", transform(filter(col("contact_extensions"), col => col("url") === CONTACT_INSTITUTIONS_SD), col => col("valueString")))
      .withColumn("contact_emails", transform(filter(col("contact_extensions"), col => col("url") === CONTACT_TYPE_SD), col => col("valueString")))
      .withColumn("website", transform(filter(col("relatedArtifact"), col => col("label") === "StudyWebsite"), col => col("url")))
      .withColumn("citation_statement", transform(filter(col("relatedArtifact"), col => col("label") === "CitationStatement"), col => col("citation")))
      .withColumn("selection_criteria", transform(filter(col("relatedArtifact"), col => col("label") === "SelectionCriteria"), col => col("citation")))
      .withColumn("funding_sources", transform(filter(col("relatedArtifact"), col => col("label") === "FundingSource"), col => col("citation")))
      .withColumn("expected_items", firstNonNull(filter(col("extension"), col => col("url") === EXPECTED_CONTENT_SD)))
      .withColumn("expected_number_participants", filter(col("expected_items")("extension"), col => col("url") === "expectedNumberParticipants")(0)("valueInteger"))
      .withColumn("expected_number_biospecimens", filter(col("expected_items")("extension"), col => col("url") === "expectedNumberBiospecimens")(0)("valueInteger"))
      .withColumn("expected_number_files", filter(col("expected_items")("extension"), col => col("url") === "expectedNumberFiles")(0)("valueInteger"))
      .withColumn("restricted_number_participants", filter(col("expected_items")("extension"), col => col("url") === "restrictedNumberParticipants")(0)("valueInteger"))
      .withColumn("restricted_number_biospecimens", filter(col("expected_items")("extension"), col => col("url") === "restrictedNumberBiospecimens")(0)("valueInteger"))
      .withColumn("restricted_number_files", filter(col("expected_items")("extension"), col => col("url") === "restrictedNumberFiles")(0)("valueInteger"))
      .withColumn("principal_investigators", transform(firstNonNull(filter(col("extension"), col => col("url") === PRINCIPAL_INVESTIGATORS_SD))("extension"), col => col("valueString")))
      .withColumn("data_categories", transform(filter(col("extension"), col => col("url") === DATA_CATEGORY_SD)("valueCoding"), col => when(
        isnull(col("display")), col("code")
      ).otherwise(col("display"))))
      .withColumn("study_designs", transform(filter(col("extension"), col => col("url") === STUDY_DESIGN_SD)("valueCoding"), col => when(
        isnull(col("display")), col("code")
      ).otherwise(col("display"))))
      .withColumn("data_collection_methods", transform(filter(col("extension"), col => col("url") === DATA_COLLECTION_METHODS_DS)("valueCoding"), col => when(
        isnull(col("display")), col("code")
      ).otherwise(col("display"))))
      .withColumn("domain", transform(col("category")("coding")(0),  col => when(
        isnull(col("display")), col("code")
      ).otherwise(col("display"))))
      .withColumn("study_code", firstNonNull(filter(col("identifier"), col => col("use") === "secondary"))("value"))
      .withColumn("access_limitations", transform(
        filter(col("extension"), col => col("url") === ACCESS_LIMITATIONS_S_D)(0)("valueCodeableConcept")("coding"),
        col => struct(col("code") as "code", col("display") as "display")))
      .withColumn("access_requirements", transform(
        filter(col("extension"), col => col("url") === ACCESS_REQUIREMENTS_S_D)(0)("valueCodeableConcept")("coding"),
        col => struct(col("code") as "code", col("display") as "display")))
      .withColumn("population", filter(col("extension"), col => col("url") === POPULATION_S_D)(0)("valueCoding")("code"))
      .withColumn("study_version", regexp_extract(filter(col("meta")("tag"), col => col("code").contains("study_version"))(0)("code"), versionExtract, 1))
      .withColumn("data_sets_ext", transform(
        filter(col("extension"), col => col("url") === DATASET_SD), col => col("extension")
      ))
      .withColumn("data_sets", transform(col("data_sets_ext"), col => struct(
        filter(col, col => col("url") === "name")(0)("valueString") as "name",
        filter(col, col => col("url") === "description")(0)("valueString") as "description"
      )))
      .withColumn("security", firstNonNull(transform(filter(col("extension"),
        col => col("url") === RESTRICTED_SD), col => when(lower(col("valueBoolean")) === "true", lit("R")).otherwise(lit("U" )))))
    ),
    Drop("extension", "category", "meta", "identifier", "data_sets_ext", "telecom", "contact_extensions", "contact", "relatedArtifact", "expected_items")
  )

  val documentreferenceMappings: List[Transformation] = List(
    Custom { input =>
      val columns = Array("id", "type", "category", "subject", "content", "context", "study_id", "fhir_id", "meta", "relatesTo")
      val df = input
        .select(columns.head, columns.tail: _*)
        .withColumn("participant_id", regexp_extract(col("subject")("reference"), patientExtract, 1))
        .withColumn("biospecimen_reference", regexp_extract(col("context")("related")(0)("reference"), specimenExtract, 1))
        .withColumn("data_type_cs",
           filter(col("type")("coding"), col => col("system") === DOCUMENT_DATA_TYPE)(0)
        )
        .withColumn("data_type", when(isnull(col("data_type_cs")("display")), col("data_type_cs")("code"))
          .otherwise(col("data_type_cs")("code"))
        )
        .withColumn("data_category_cs",
              filter(col("category")(0)("coding"), col => col("system") === DOCUMENT_DATA_CATEGORY)(0)
        )
        .withColumn("data_category", when(isnull(col("data_category_cs")("display")), col("data_category_cs")("code"))
          .otherwise(col("data_category_cs")("code"))
        )
        .withColumn("content_exp", explode(col("content")))
        .withColumn("file_size", retrieveSize(firstNonNull(filter(col("content_exp")("attachment")("extension"), col => col("url") === DOCUMENT_SIZE_S_D))("fileSize")))
        .withColumn("ferload_url", col("content_exp")("attachment")("url"))
        .withColumn("file_hash", col("content_exp")("attachment")("hash"))
        .withColumn("file_name", col("content_exp")("attachment")("title"))
        .withColumn("file_format", col("content_exp")("format")("code"))
        .withColumn("dataset", regexp_extract(filter(col("meta")("tag"), col => col("system") === DATASETS_CS)(0)("code"), datasetExtract, 1))
        .withColumn("security", filter(col("meta")("security"), col => col("system") === SYSTEM_CONFIDENTIALITY)(0)("code"))
        .withColumn("relates_to", substring_index(col("relatesTo")("target")("reference")(0), "/", -1))
        .groupBy(columns.head, columns.tail ++ Array("participant_id", "biospecimen_reference", "data_type", "data_category", "dataset", "security", "relates_to"): _*)
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
    Drop("id", "type", "category", "subject", "content", "context", "meta", "relatesTo")
  )

  val groupMappings: List[Transformation] = List(
    Custom(_
      .select("study_id", "fhir_id", "code", "member", "identifier")
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
