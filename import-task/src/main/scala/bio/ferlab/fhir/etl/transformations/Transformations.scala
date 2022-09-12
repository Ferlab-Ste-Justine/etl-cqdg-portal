package bio.ferlab.fhir.etl.transformations

import bio.ferlab.datalake.spark3.transformation.{Custom, Drop, Transformation}
import bio.ferlab.fhir.etl.Utils.{extractFirstForSystem, _}
import bio.ferlab.fhir.etl._
import org.apache.spark.sql.Column
import org.apache.spark.sql.functions.{col, collect_list, explode, filter, regexp_extract, struct, when, _}
import org.apache.spark.sql.types.BooleanType

object Transformations {

  val patternPractitionerRoleResearchStudy = "PractitionerRole\\/([0-9]+)"
  val conditionTypeR = "^https:\\/\\/[A-Za-z0-9-_.\\/]+\\/([A-Za-z0-9]+)"

  val officialIdentifier: Column = extractOfficial(col("identifier"))

  val patientMappings: List[Transformation] = List(
    Custom(_
      .select("fhir_id", "study_id", "release_id", "gender", "ethnicity", "identifier", "race")
      .withColumn("external_id", filter(col("identifier"), c => c("system").isNull)(0)("value"))
      // TODO is_proband
      .withColumn("participant_id", officialIdentifier)
      .withColumn("race_omb", ombCategory(col("race.ombCategory")))
      .withColumn("ethnicity", ombCategory(col("ethnicity.ombCategory")))
      .withColumn("race", when(col("race_omb").isNull && lower(col("race.text")) === "more than one race", upperFirstLetter(col("race.text"))).otherwise(col("race_omb")))
    ),
    Drop("identifier", "race_omb")
  )

  val researchSubjectMappings: List[Transformation] = List(
    Custom(_
      .select("fhir_id", "release_id", "identifier", "study_id")
      .withColumn("external_id", filter(col("identifier"), c => c("system").isNull)(0)("value"))
      .withColumn("participant_id", officialIdentifier)
    ),
    Drop("identifier")
  )

  val age_at_bio_collection_on_set_intervals = Seq((0, 5), (5, 10), (10, 20), (20, 30), (30, 40), (40, 50), (50, 60), (60, 70), (70, 80))

  val biospecimenMappings: List[Transformation] = List(
    Custom { _
      .select("fhir_id", "extension", "identifier", "subject", "study_id", "release_id", "type")
      .where(size(col("parent")) === 0)
      .withColumn("subject", regexp_extract(col("subject")("reference"), patientExtract, 1))
      .withColumn("biospecimen_tissue_source",
        transform(col("type")("coding"), col => struct(col("system") as "system", col("code") as "code"))(0))
      //todo fix value or age (in bytes)
      .withColumn("age_biospecimen_collection", extractValueAge(col("extension")).cast("struct<value:long,unit:string>"))
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

  val observationVitalStatusMappings: List[Transformation] = List(
    Custom(_
      .select("fhir_id", "release_id", "subject", "valueCodeableConcept", "identifier", "_effectiveDateTime", "study_id")
      .withColumn("participant_fhir_id", extractReferenceId(col("subject")("reference")))
      .withColumn("vital_status", col("valueCodeableConcept")("text"))
      .withColumn("observation_id", officialIdentifier)
      .withColumn("age_at_event_days", struct(
        col("_effectiveDateTime")("effectiveDateTime")("offset")("value") as "value",
        col("_effectiveDateTime")("effectiveDateTime")("offset")("unit") as "units",
        extractFirstForSystem(col("_effectiveDateTime")("effectiveDateTime")("event")("coding"), Seq("http://snomed.info/sct"))("display") as "from"
      ))
      // TODO external_id
    ),
    Drop("subject", "valueCodeableConcept", "identifier", "_effectiveDateTime", "parent_0")
  )

  val observationFamilyRelationshipMappings: List[Transformation] = List(
    Custom(_
      .select("fhir_id", "release_id", "subject", "identifier", "focus", "valueCodeableConcept", "meta")
      .withColumn("study_id", col("meta")("tag")(0)("code"))
      .withColumn("observation_id", officialIdentifier)
      .withColumn("participant1_fhir_id", extractReferenceId(col("subject")("reference")))
      .withColumn("participant2_fhir_id", extractReferencesId(col("focus")("reference"))(0))
      .withColumn(
        "participant1_to_participant_2_relationship",
        extractFirstForSystem(col("valueCodeableConcept")("coding"), ROLE_CODE_URL)("display")
      )
      // TODO external_id
    ),
    Drop("subject", "identifier", "focus", "valueCodeableConcept", "meta")
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
//      .select("fhir_id", "study_id", "release_id", "identifier", "code", "subject", "verificationStatus", "_recordedDate")
      .select("fhir_id", "study_id", "release_id", "identifier", "code", "subject")
      .withColumn("phenotype_id", officialIdentifier)
      .withColumn("condition_coding", codingClassify(col("code")("coding")).cast("array<struct<category:string,code:string>>"))
      .withColumn("source_text", col("code")("text"))
      .withColumn("participant_fhir_id", extractReferenceId(col("subject")("reference")))
//      .withColumn("observed", col("verificationStatus")("coding")(0)("code"))
//      .withColumn("age_at_event", struct(
//        col("_recordedDate")("recordedDate")("offset")("value") as "value",
//        col("_recordedDate")("recordedDate")("offset")("unit") as "units",
//        //todo same for include?
//        extractFirstForSystem(col("_recordedDate")("recordedDate")("event")("coding"), Seq("http://snomed.info/sct"))("display") as "from"
//      ))
      // TODO snomed_id_phenotype
      // TODO external_id
    ),
//    Drop("identifier", "code", "subject", "verificationStatus", "_recordedDate")
  )

  val organizationMappings: List[Transformation] = List(
    Custom(_
      .select("fhir_id", "release_id", "study_id", "identifier", "name")
      .withColumn("organization_id", officialIdentifier)
      .withColumn("institution", col("name"))
    ),
    Drop("identifier", "name")
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
      .select("*")
      .where(exists(col("identifier"), identifier => identifier("system") === "https://kf-api-dataservice.kidsfirstdrc.org/families/" ) || exists(col("code.coding"), code => code("code") === "FAMMEMB"  ) )
      .withColumn("external_id", filter(col("identifier"), c => c("system").isNull)(0)("value"))
      .withColumn("family_id", officialIdentifier)
      .withColumn("exploded_member", explode(col("member")))
      .withColumn("exploded_member_entity", extractReferenceId(col("exploded_member")("entity")("reference")))
      .withColumn("exploded_member_inactive", col("exploded_member")("inactive"))
      .withColumn("family_members", struct("exploded_member_entity", "exploded_member_inactive"))
      .groupBy("fhir_id", "study_id", "family_id", "external_id", "type", "release_id")
      .agg(
        collect_list("family_members") as "family_members",
        collect_list("exploded_member_entity") as "family_members_id"
      )
    ),
    Drop()
  )

  val extractionMappings: Map[String, List[Transformation]] = Map(
    "patient" -> patientMappings,
    "biospecimen" -> biospecimenMappings,
    "sample_registration" -> sampleRegistrationMappings,
    "vital_status" -> observationVitalStatusMappings,
    "family_relationship" -> observationFamilyRelationshipMappings,
    "phenotype" -> conditionPhenotypeMappings,
    "diagnosis" -> conditionDiagnosisMappings,
    "research_subject" -> researchSubjectMappings,
    "research_study" -> researchstudyMappings,
    "group" -> groupMappings,
    "document_reference" -> documentreferenceMappings,
    "organization" -> organizationMappings
  )

}
