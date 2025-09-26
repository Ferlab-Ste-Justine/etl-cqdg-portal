package bio.ferlab.fhir

package object etl {
  //SYSTEM URL
  val SYSTEM_URL_CQDG = "https://fhir.cqdg.ca"
  val SYSTEM_MONDO = "http://purl.obolibrary.org/obo/mondo.owl"
  val SYSTEM_ICD = "http://terminology.hl7.org/CodeSystem/icd10-CA"
  val SYSTEM_CONFIDENTIALITY = "http://terminology.hl7.org/CodeSystem/v3-Confidentiality"

//  *********** CODE SYSTEMS **************
  val DOCUMENT_DATA_TYPE = s"$SYSTEM_URL_CQDG/CodeSystem/data-type"
  val DOCUMENT_DATA_CATEGORY = s"$SYSTEM_URL_CQDG/CodeSystem/data-category"
  val TASK_BIO_INFO = s"$SYSTEM_URL_CQDG/CodeSystem/bioinfo-analysis-code"
  val DATASETS_CS = s"$SYSTEM_URL_CQDG/CodeSystem/cqdg-dataset-cs"
  val SOURCE_CS = s"$SYSTEM_URL_CQDG/CodeSystem/sequencing-experiment-source"
  val SELECTION_CS = s"$SYSTEM_URL_CQDG/CodeSystem/sequencing-experiment-selection"

  //  *********** STRUCTURE DEFINITION **************
  val AGE_AT_EVENT_S_D = s"$SYSTEM_URL_CQDG/StructureDefinition/AgeAtEvent"
  val SEQUENCING_EXPERIMENT_S_D = s"$SYSTEM_URL_CQDG/StructureDefinition/SequencingExperimentExtension"
  val WORKFLOW_S_D = s"$SYSTEM_URL_CQDG/StructureDefinition/WorkflowExtension"
  val TASK_SAMPLE_S_D = s"$SYSTEM_URL_CQDG/StructureDefinition/SampleExtension"
  val DOCUMENT_SIZE_S_D = s"$SYSTEM_URL_CQDG/StructureDefinition/FullSizeExtension"
  val AGE_AT_RECRUITMENT_S_D = s"$SYSTEM_URL_CQDG/StructureDefinition/AgeAtRecruitment"
  // Patient
  val ETHNICITY_S_D = s"$SYSTEM_URL_CQDG/StructureDefinition/QCEthnicity"
  val GENDER_S_D = s"$SYSTEM_URL_CQDG/StructureDefinition/Gender"
  val CANCER_ANATOMIC_LOCATION_S_D = s"$SYSTEM_URL_CQDG/StructureDefinition/CancerBiospecimenAnatomicLocation"
  val TUMOR_HISTOLOGICAL_TYPE_S_D = s"$SYSTEM_URL_CQDG/StructureDefinition/TumorHistologicalType"
  val TUMOR_NORMAL_DESIGNATION_S_D = s"$SYSTEM_URL_CQDG/StructureDefinition/TumorNormalDesignation"
  val CANCER_BIOSPECIMEN_TYPE_S_D = s"$SYSTEM_URL_CQDG/StructureDefinition/CancerBiospecimenType"
  val SEX_AT_BIRTH_S_D = s"$SYSTEM_URL_CQDG/StructureDefinition/SexAtBirth"
  val RACE_S_D = s"$SYSTEM_URL_CQDG/StructureDefinition/Race"
  val VITAL_STATUS_S_D = s"$SYSTEM_URL_CQDG/StructureDefinition/VitalStatus"
  val AGE_OF_DEATH_S_D = s"$SYSTEM_URL_CQDG/StructureDefinition/AgeOfDeath"
  // ResearchStudy
  val ACCESS_LIMITATIONS_S_D = s"$SYSTEM_URL_CQDG/StructureDefinition/AccessLimitations"
  val ACCESS_REQUIREMENTS_S_D = s"$SYSTEM_URL_CQDG/StructureDefinition/AccessRequirements"
  val POPULATION_S_D = s"$SYSTEM_URL_CQDG/StructureDefinition/ResearchStudy/population"
  val DATASET_SD = s"$SYSTEM_URL_CQDG/StructureDefinition/dataset"
  val CONTACT_INSTITUTIONS_SD = s"$SYSTEM_URL_CQDG/StructureDefinition/Contact/ContactInstitution"
  val CONTACT_TYPE_SD = s"$SYSTEM_URL_CQDG/StructureDefinition/Contact/ContactTypes"
  val RESTRICTED_SD = s"$SYSTEM_URL_CQDG/StructureDefinition/Restricted"
  val EXPECTED_CONTENT_SD = s"$SYSTEM_URL_CQDG/StructureDefinition/ResearchStudy/ExpectedContent"
  val PRINCIPAL_INVESTIGATORS_SD = s"$SYSTEM_URL_CQDG/StructureDefinition/PrincipalInvestigators"
  val DATA_CATEGORY_SD = s"$SYSTEM_URL_CQDG/StructureDefinition/DataCategoryExtension"
  val STUDY_DESIGN_SD = s"$SYSTEM_URL_CQDG/StructureDefinition/StudyDesignExtension"
  val DATA_COLLECTION_METHODS_DS = s"$SYSTEM_URL_CQDG/StructureDefinition/DataCollectionMethodExtension"
  val RESEARCH_PROGRAM_SD = s"$SYSTEM_URL_CQDG/StructureDefinition/research-program"
  val RESEARCH_PROGRAM_RELATED_ARTIFACT_SD = s"$SYSTEM_URL_CQDG/StructureDefinition/research-program-related-artifact"
  val RESEARCH_PROGRAM_CONTACT_SD = s"$SYSTEM_URL_CQDG/StructureDefinition/research-program-contact"
  val RESEARCH_PROGRAM_PARTNER_SD = s"$SYSTEM_URL_CQDG/StructureDefinition/research-program-partner"
}
