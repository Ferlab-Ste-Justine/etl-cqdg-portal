package bio.ferlab.fhir.etl.transformations

import bio.ferlab.datalake.spark3.transformation.{Custom, Drop, Transformation}
import bio.ferlab.fhir.etl.Utils._
import bio.ferlab.fhir.etl._
import org.apache.spark.sql.Column
import org.apache.spark.sql.functions.{col, _}

object Transformations {

  val officialIdentifier: Column = extractOfficial(col("identifier"))

  val patientMappings: List[Transformation] = List(
    Custom(_
      .select("fhir_id", "study_id", "release_id", "identifier", "extension", "gender", "deceasedBoolean")
      .withColumn("age_at_recruitment", firstNonNull(transform(
        filter(col("extension"),col => col("url") === SYS_AGE_AT_RECRUITMENT)("valueAge"),
        col => struct(col("value") as "value", col("unit") as "unit")
      )))
      .withColumn("ethnicity",
        firstNonNull(firstNonNull(filter(col("extension"),col => col("url") === SYS_ETHNICITY_URL)("valueCodeableConcept"))("coding"))("code")
      )
      .withColumn("submitter_participant_id", firstNonNull(filter(col("identifier"), col => col("use") === "secondary"))("value"))

    ),
    Drop("identifier", "extension")
  )

  val biospecimenMappings: List[Transformation] = List(
    Custom { _
      .select("fhir_id", "extension", "identifier", "subject", "study_id", "release_id", "type")
      .where(size(col("parent")) === 0)
      .withColumn("subject", regexp_extract(col("subject")("reference"), patientExtract, 1))
      .withColumn("biospecimen_tissue_source",
        transform(col("type")("coding"), col => struct(col("system") as "system", col("code") as "code"))(0))
      //todo fix value or age (in bytes)
      .withColumn("age_biospecimen_collection", extractValueAge("https://fhir.cqdg.ferlab.bio/StructureDefinition/Specimen/ageBiospecimenCollection")(col("extension")).cast("struct<value:long,unit:string>"))
      .withColumn("submitter_participant_id", firstNonNull(filter(col("identifier"), col => col("use") === "secondary")("value")))
    },
    Drop("type", "extension", "identifier")
  )

  val sampleRegistrationMappings: List[Transformation] = List(
    Custom { _
      .select("identifier", "type", "subject", "parent", "study_id", "release_id", "fhir_id")
      .where(size(col("parent")) > 0)
      .withColumn("sample_type",
        transform(col("type")("coding"), col => struct(col("system") as "system", col("code") as "code"))(0))
      .withColumn("submitter_participant_id", firstNonNull(filter(col("identifier"), col => col("use") === "secondary")("value")))
      .withColumn("subject",  regexp_extract(col("subject")("reference"), patientExtract, 1))
      .withColumn("parent", firstNonNull(transform(col("parent"),  col => regexp_extract(col("reference"), specimenExtract, 1))))
    },
    Drop("identifier", "type")
  )

  val observationFamilyRelationshipMappings: List[Transformation] = List(
    Custom(_
      .select("study_id", "release_id", "fhir_id", "code", "subject", "focus", "valueCodeableConcept", "category")
      .where(col("code")("coding")(0)("code") === "FAMM")
      .withColumn("subject",  regexp_extract(col("subject")("reference"), patientExtract, 1))
      .withColumn("focus", firstNonNull(transform(col("focus"),  col => regexp_extract(col("reference"), patientExtract, 1))))
      .withColumn("relationship_to_proband", firstNonNull(transform(col("valueCodeableConcept")("coding"), col => col("code"))))
      .withColumn("category", col("category")(0)("coding")(0)("code"))
    ),
    Drop("code", "valueCodeableConcept")
  )

  val conditionDiagnosisMappings: List[Transformation] = List(
    Custom(_
      .select("identifier", "code", "subject", "onsetAge", "study_id", "release_id", "fhir_id")
      .withColumn("diagnosis_source_text", col("code")("text"))
      .withColumn("diagnosis_mondo_code",
        firstNonNull(filter(col("code")("coding"), col => col("system").equalTo("http://purl.obolibrary.org/obo/mondo.owl"))("code")))
      .withColumn("diagnosis_ICD_code", firstNonNull(filter(col("code")("coding"), col => col("system").isNull)("code")))
      .withColumn("subject", regexp_extract(col("subject")("reference"), patientExtract, 1))
      //todo fix value or age (in bytes)
      .withColumn("age_at_diagnosis", struct(col("onsetAge")("value") as "value", col("onsetAge")("unit") as "unit"))
    ),
    Drop("identifier", "code", "onsetAge")
  )

  val conditionPhenotypeMappings: List[Transformation] = List(
    Custom(_
      .select("study_id", "release_id", "fhir_id", "code", "valueCodeableConcept", "subject")
      .where(col("code")("coding")(0)("code") === "PHEN")
      .withColumn("phenotype_source_text", col("code")("text"))
      .withColumn("phenotype_HPO_code",
         firstNonNull(transform(col("valueCodeableConcept")("coding"), col => struct(col("system") as "system", col("code") as "code")))
      )
      .withColumn("cqdg_participant_id", regexp_extract(col("subject")("reference"), patientExtract, 1))
    ),
    Drop("code", "valueCodeableConcept", "subject")
  )


  val researchstudyMappings: List[Transformation] = List(
    Custom(_
      .select("fhir_id", "keyword", "release_id", "study_id", "description", "contact", "category", "status", "title", "extension")
      .withColumn("keyword", extractKeywords(col("keyword")))
      .withColumn(
        "contact", transform(col("contact"), col => struct(col("telecom")(0)("system") as "type", col("telecom")(0)("value") as "value"))(0)
      )
      .withColumn(
        "category", transform(col("category"), col => struct(col("coding")(0)("system") as "system", col("coding")(0)("code") as "code"))
      )
      .withColumn("access_limitations",
        extractValueCodeableConcept(ACCESS_LIMITATIONS_URL)(col("extension")).cast("array<struct<code:string,system:string>>")
      )
      .withColumn("access_requirements",
        extractValueCodeableConcept(ACCESS_REQUIREMENTS_URL)(col("extension")).cast("array<struct<code:string,system:string>>")
      )
    ),
    Drop("extension")
  )

  val documentreferenceMappings: List[Transformation] = List(
    Custom { input =>
      val df = input
        .select("fhir_id", "study_id", "release_id", "category", "securityLabel", "content", "type", "identifier", "subject", "context", "docStatus", "relatesTo")
        .withColumn("access_urls", col("content")("attachment")("url")(0))
        .withColumn("acl", extractAclFromList(col("securityLabel")("text"), col("study_id")))
        .withColumn("controlled_access", firstSystemEquals(flatten(col("securityLabel.coding")), SYS_DATA_ACCESS_TYPES)("display"))
        .withColumn("data_type", firstSystemEquals(col("type")("coding"), SYS_DATA_TYPES)("display"))
        .withColumn("data_category", firstSystemEquals(flatten(col("category")("coding")), SYS_DATA_CATEGORIES)("display"))
        .withColumn("experiment_strategy", firstSystemEquals(flatten(col("category")("coding")), SYS_EXP_STRATEGY)("display"))
        .withColumn("external_id", col("content")(0)("attachment")("url"))
        .withColumn("file_format", firstNonNull(col("content")("format")("display")))
        .withColumn("file_name", sanitizeFilename(firstNonNull(col("content")("attachment")("title"))))
        .withColumn("file_id", officialIdentifier)
        .withColumn("hashes", extractHashes(col("content")(0)("attachment")("hashes")))
        .withColumn("is_harmonized", retrieveIsHarmonized(col("content")(0)("attachment")("url")))
        .withColumn("latest_did", split(col("content")("attachment")("url")(0), "\\/\\/")(2))
        .withColumn("repository", retrieveRepository(col("content")("attachment")("url")(0)))
        .withColumn("size", retrieveSize(col("content")(0)("attachment")("fileSize")))
        .withColumn("urls", col("content")(0)("attachment")("url"))
        .withColumn("participant_fhir_id", extractReferenceId(col("subject")("reference")))
        .withColumn("specimen_fhir_ids", extractReferencesId(col("context")("related")("reference")))
        .withColumnRenamed("docStatus", "status")
        .withColumn("relate_to", extractReferencesId(col("relatesTo.target.reference"))(0))

      val indexes = df.as("index").where(col("file_format").isin("crai", "tbi", "bai"))
      val files = df.as("file").where(not(col("file_format").isin("crai", "tbi", "bai")))

      files
        .join(indexes, col("index.relate_to") === col("file.fhir_id"), "left_outer")
        .select(
          col("file.*"),
          when(
            col("index.relate_to").isNull, lit(null)
          )
            .otherwise(
              struct(col("index.fhir_id") as "fhir_id", col("index.file_name") as "file_name",
                col("index.file_id") as "file_id", col("index.hashes") as "hashes",
                col("index.urls") as "urls", col("index.file_format") as "file_format",
                col("index.size") as "size"
              )
            ) as "index"
        )

    }
    ,
    Drop("securityLabel", "content", "type", "identifier", "subject", "context", "relates_to", "relate_to")
  )

  val groupMappings: List[Transformation] = List(
    Custom(_
      .select("study_id", "release_id", "fhir_id", "code", "member", "identifier")
      .withColumn("family_type", firstNonNull(transform(col("code")("coding"), col => col("code"))))
      .withColumn("family_members", transform(col("member"), col => regexp_extract(col("entity")("reference"), patientExtract, 1)))
      .withColumn("submitter_family_id", col("identifier")(0)("value"))
      //fixme missing 'cqdg_participant_id' (see xls file) ?? clarify...
    ),
    Drop("code", "member", "identifier")
  )

  val extractionMappings: Map[String, List[Transformation]] = Map(
    "patient" -> patientMappings,
    "biospecimen" -> biospecimenMappings,
    "sample_registration" -> sampleRegistrationMappings,
    "family_relationship" -> observationFamilyRelationshipMappings,
    "phenotype" -> conditionPhenotypeMappings,
    "diagnosis" -> conditionDiagnosisMappings,
    "research_study" -> researchstudyMappings,
    "group" -> groupMappings,
    "document_reference" -> documentreferenceMappings,
  )

}
