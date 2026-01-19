package bio.ferlab.fhir.etl.transformations

import bio.ferlab.datalake.spark3.transformation.{Custom, Drop, Transformation}
import bio.ferlab.fhir.etl.Utils._
import bio.ferlab.fhir.etl._
import org.apache.spark.sql.Column
import org.apache.spark.sql.functions.{col, _}

object Transformations {

  val patientMappings: List[Transformation] = List(
    Custom(_
      .select("fhir_id", "study_id", "identifier", "extension", "meta")
      .withColumn("age_at_recruitment", ageFromExtension(col("extension"), AGE_AT_RECRUITMENT_S_D))
      .withColumn("ethnicity", extractFromExtensionValueCoding(col("extension"), ETHNICITY_S_D))
      .withColumn("submitter_participant_id", firstNonNull(filter(col("identifier"), col => col("use") === "secondary"))("value"))
      .withColumn("vital_status", extractFromExtensionValueCoding(col("extension"), VITAL_STATUS_S_D))
      .withColumn("age_of_death", ageFromExtension(col("extension"), AGE_OF_DEATH_S_D))
      .withColumn("genderExt", firstNonNull(filter(col("extension"),col => col("url") === GENDER_S_D))("extension"))
      .withColumn("gender", extractDemographicStruct(col("genderExt"), "gender"))
      .withColumn("sexAtBirthExt", firstNonNull(filter(col("extension"),col => col("url") === SEX_AT_BIRTH_S_D))("extension"))
      .withColumn("sex_at_birth", extractDemographicStruct(col("sexAtBirthExt"), "sexAtBirth"))
      .withColumn("sex", lower(extractFromExtensionValueCoding(col("sexAtBirthExt"), "sexAtBirth")))
      .withColumn("raceExt", firstNonNull(filter(col("extension"),col => col("url") === RACE_S_D))("extension"))
      .withColumn("race", extractDemographicStruct(col("raceExt"), "race"))
      .withColumn("security", filter(col("meta")("security"), col => col("system") === SYSTEM_CONFIDENTIALITY)(0)("code"))
    ),
    Drop("identifier", "extension", "meta", "genderExt", "sexAtBirthExt", "raceExt")
  )

  val taskMappings: List[Transformation] = List(
    Custom(_
      .select("fhir_id", "study_id", "output", "code", "for", "owner", "extension")
      .withColumn("bio_informatic_analysis", filter(col("code")("coding"), col => col("system") === TASK_BIO_INFO)(0)("display"))
      .withColumn("seq_exp", filter(col("extension"), col => col("url") === SEQUENCING_EXPERIMENT_S_D)(0))
      .withColumn("workflow", filter(col("extension"), col => col("url") === WORKFLOW_S_D)(0))
      .withColumn("task_sample_ext", filter(col("extension"), col => col("url") === TASK_SAMPLE_S_D)(0))
      .withColumn("lab_aliquot_ids", filter(col("task_sample_ext")("extension"), col => col("url") === "labAliquotId")("valueString"))
      .withColumn("ldm_sample_id", filter(col("task_sample_ext")("extension"), col => col("url") === "ldmSampleId")(0)("valueString"))
      .withColumn("run_ids", filter(col("seq_exp")("extension"), col => col("url") === "runIds")("valueString"))
      .withColumn(
        "run_dates",
        transform(
          filter(col("seq_exp")("extension"), col => col("url") === "runDates")("valueDate"),
          date => date_format(date_from_unix_date(date), "yyyy-MM-dd")
        )
      )
      .withColumn("read_length", split(filter(col("seq_exp")("extension"), col => col("url") === "readLength")(0)("valueString"), ",")(0))
      .withColumn("is_paired_end", filter(col("seq_exp")("extension"), col => col("url") === "isPairedEnd")(0)("valueBoolean"))
      // date extracted from fhir are one day before, Likely due to improper time zone setup on the fhir server. This should be fixed
      // and date_add function removed...
      .withColumn("capture_kit", filter(col("seq_exp")("extension"), col => col("url") === "captureKit")(0)("valueString"))
      .withColumn("platform", filter(col("seq_exp")("extension"), col => col("url") === "platform")(0)("valueString"))
      .withColumn("target_capture_kit", filter(col("seq_exp")("extension"), col => col("url") === "targetCaptureKit")(0)("valueString"))
      .withColumn("protocol", filter(col("seq_exp")("extension"), col => col("url") === "protocol")(0)("valueString"))
      .withColumn("target_loci", filter(col("seq_exp")("extension"), col => col("url") === "targetLoci")(0)("valueString"))
      .withColumn(
        "experimental_strategy_1",
        struct(
          filter(col("seq_exp")("extension"), col => col("url") === "experimentalStrategy")(0)("valueCoding")("code").as("code"),
          filter(col("seq_exp")("extension"), col => col("url") === "experimentalStrategy")(0)("valueCoding")("display").as("display")
        )
      )
      .withColumn( //FIXME remove this field after all studies are updated (replace _1)
        "experimental_strategy",
        filter(col("seq_exp")("extension"), col => col("url") === "experimentalStrategy")(0)("valueCoding")("code")
      )
      .withColumn("sequencer_id", filter(col("seq_exp")("extension"), col => col("url") === "sequencerId")(0)("valueString"))
      .withColumn(
        "source",
        transform(
          filter(col("seq_exp")("extension"), col => col("url") === "source")("valueCoding"),
          coding => struct(
            coding("code").as("code"),
            coding("display").as("display")
          )
        )(0)
      )
      .withColumn(
        "selection",
        transform(
          filter(col("seq_exp")("extension"), col => col("url") === "selection")("valueCoding"),
          coding => struct(
            coding("code").as("code"),
            coding("display").as("display")
          )
        )(0)
      )
      .withColumn("genome_build", filter(col("workflow")("extension"), col => col("url") === "genomeBuild")(0)("valueCoding")("code"))
      .withColumn("pipelines", filter(col("workflow")("extension"), col => col("url") === "pipeline")("valueString"))
      .withColumn("_for", regexp_extract(col("for")("reference"), patientExtract, 1))
      .withColumn("owner", regexp_extract(col("owner")("reference"), organizationExtract, 1))
      .withColumn("analysis_files", transform(col("output"), col => struct(col("type")("coding")(0)("code") as "data_type", col("valueReference")("reference") as "file_id")))
    ),
    Drop("seq_exp", "extension", "workflow", "task_sample_ext", "code", "output", "for")
  )

  private def firstValue(contact: Column, key: String, valueType: String = "valueString"): Column =
    firstNonNull(filter(contact("extension"), ext => ext("url") === key))(valueType)


  val listMappings: List[Transformation] = List(
    Custom(_
      .select("fhir_id", "study_id", "identifier", "title", "entry", "extension")
      .withColumn("name_en", col("title"))
      .withColumn("study_codes", transform(col("entry"), e => regexp_extract(e("item")("reference"), ".*/(.*)", 1)))
      .withColumn("program_id", col("identifier")(0)("value"))
      .withColumn("research_program_ext", firstNonNull(filter(col("extension"), col => col("url") === RESEARCH_PROGRAM_SD))("extension"))
      .withColumn("name_fr", filter(col("research_program_ext"), col => col("url") === "nameFR")(0)("valueString"))
      .withColumn("description_fr", filter(col("research_program_ext"), col => col("url") === "descriptionFR")(0)("valueString"))
      .withColumn("description_en", filter(col("research_program_ext"), col => col("url") === "descriptionEN")(0)("valueString"))
      .withColumn("research_program_related_artifact_raw", filter(col("research_program_ext"), col => col("url") === RESEARCH_PROGRAM_RELATED_ARTIFACT_SD)(0)("extension"))
      .withColumn("research_program_related_artifact", struct(
        firstNonNull(filter(col("research_program_related_artifact_raw"),
          col => col("url") === "website"))("valueUrl") as "website",
        firstNonNull(filter(col("research_program_related_artifact_raw"),
          col => col("url") === "citationStatement"))("valueString") as "citation_statement",
        firstNonNull(filter(col("research_program_related_artifact_raw"),
          col => col("url") === "logo"))("valueUrl") as "logo_url",
      ))
      .withColumn("research_program_contacts_raw", filter(col("research_program_ext"), col => col("url") === RESEARCH_PROGRAM_CONTACT_SD))
      .withColumn(
        "research_program_contacts",
        transform(
          col("research_program_contacts_raw"),
          contact =>
            struct(
              firstValue(contact, "name").as("name"),
              firstValue(contact, "contactInstitution").as("institution"),
              firstValue(contact, "ProgramRoleEN").as("role_en"),
              firstValue(contact, "ProgramRoleFR").as("role_fr"),
              firstNonNull(
                filter(contact("extension"), ext => ext("url") === RESEARCH_PROGRAM_RELATED_ARTIFACT_SD)
              )("extension")(0)("valueUrl").as("picture_url"),
                 transform(filter(contact("extension"), ext => ext("url") === "telecom")("valueContactPoint"),
                   col => struct(col("value"), col("system"))).as("telecom")
            )
        )
      )
      .withColumn("research_program_partners_raw", filter(col("research_program_ext"), col => col("url") === RESEARCH_PROGRAM_PARTNER_SD))
      .withColumn(
        "research_program_partners",
        transform(
          col("research_program_partners_raw"),
          contact =>
            struct(
              firstValue(contact, "name").as("name"),
              firstValue(contact, "logo", "valueUrl").as("logo"),
              firstValue(contact, "rank", "valueInteger").as("rank"),
            )
        )
      )
    ),
    Drop("title", "entry", "extension", "identifier", "research_program_related_artifact_raw",
      "research_program_contacts_raw", "research_program_ext", "research_program_partners_raw")
  )

  val biospecimenMappings: List[Transformation] = List(
    Custom { _
      .select("fhir_id", "extension", "identifier", "subject", "study_id", "type", "meta", "parent")
      .where(size(col("parent")) === 0)
      .withColumn("subject", regexp_extract(col("subject")("reference"), patientExtract, 1))
      .withColumn("biospecimen_tissue_source",
        transform(col("type")("coding"), col => struct(col("system") as "system", col("code") as "code", col("display") as "display"))(0))
      .withColumn("tumor_normal_designation", transform(
        filter(col("extension"), col => col("url") === TUMOR_NORMAL_DESIGNATION_S_D)(0)("valueCodeableConcept")("coding"),
        col => when(col("display").isNull, col("code")).otherwise(col("display"))
      )(0))
      .withColumn(
        "cancer_biospecimen_type",
        struct(
          firstNonNull(
            filter(col("extension"), col => col("url") === CANCER_BIOSPECIMEN_TYPE_S_D)
          )("valueCodeableConcept")("coding")(0)("code").as("code"),
          firstNonNull(
            filter(col("extension"), col => col("url") === CANCER_BIOSPECIMEN_TYPE_S_D)
          )("valueCodeableConcept")("coding")(0)("display").as("display")
        )
      )
      .withColumn("cancer_anatomic_location", extractTumorNCITStruct(CANCER_ANATOMIC_LOCATION_S_D))
      .withColumn("tumor_histological_type", extractTumorNCITStruct(TUMOR_HISTOLOGICAL_TYPE_S_D))

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
        transform(col("type")("coding"), col => struct(col("system") as "system", col("code") as "code"))(0))
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
      .filter(array_contains(col("code")("coding")("code"),"Disease-Status"))
      .withColumn("subject", regexp_extract(col("subject")("reference"), patientExtract, 1))
      .withColumn("disease_status", col("valueCodeableConcept")("coding")("code")(0))
    ),
    Drop("code", "valueCodeableConcept")
  )

  val observationFamilyRelationshipMappings: List[Transformation] = List(
    Custom(_
      .select("study_id", "fhir_id", "code", "subject", "focus", "valueCodeableConcept", "category")
      .withColumnRenamed("fhir_id", "internal_family_relationship_id")
      .where(col("code")("coding")(0)("code") === "Family-relationship")
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


  private val citationsOf: String => Column = (label: String) => filter(col("relatedArtifact"), c => c("label") === label)("citation")

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
      .withColumn("website", transform(filter(col("relatedArtifact"), col => col("label") === "StudyWebsite"), col => col("url"))(0))
      .withColumn("citation_statement", citationsOf("CitationStatement")(0))
      .withColumn("selection_criteria", citationsOf("SelectionCriteria")(0))
      .withColumn("funding_sources", citationsOf("FundingSource"))
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
        .withColumn("biospecimen_reference", transform(col("context")("related"), col => regexp_extract(col("reference"), specimenExtract, 1)))
        .withColumn("data_type_cs",
           filter(col("type")("coding"), col => col("system") === DOCUMENT_DATA_TYPE)(0)
        )
        .withColumn("data_type", when(isnull(col("data_type_cs")("display")), col("data_type_cs")("code"))
          .otherwise(col("data_type_cs")("display"))
        )
        .withColumn("data_category_cs",
              filter(col("category")(0)("coding"), col => col("system") === DOCUMENT_DATA_CATEGORY)(0)
        )
        .withColumn("data_category", when(isnull(col("data_category_cs")("display")), col("data_category_cs")("code"))
          .otherwise(col("data_category_cs")("display"))
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
    "list" -> listMappings,
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
  )

}