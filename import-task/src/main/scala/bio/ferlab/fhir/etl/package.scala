package bio.ferlab.fhir

package object etl {
  //SYSTEM URL
  val SYSTEM_URL_CQDG = "https://fhir.cqdg.ca"

//  *********** CODE SYSTEMS **************
  val DOCUMENT_DATA_TYPE = s"$SYSTEM_URL_CQDG/CodeSystem/data-type"
  val DOCUMENT_DATA_CATEGORY = s"$SYSTEM_URL_CQDG/CodeSystem/data-category"
  val TASK_BIO_INFO = s"$SYSTEM_URL_CQDG/CodeSystem/bioinfo-analysis-code"

  //  *********** STRUCTURE DEFINITION **************
  val AGE_AT_PHENOTYPE_S_D = s"$SYSTEM_URL_CQDG/StructureDefinition/Observation/AgeAtPhenotype"
  val SEQUENCING_EXPERIMENT_S_D = s"$SYSTEM_URL_CQDG/StructureDefinition/SequencingExperimentExtension"
  val WORKFLOW_S_D = s"$SYSTEM_URL_CQDG/StructureDefinition/WorkflowExtension"
  val DOCUMENT_SIZE_S_D = s"$SYSTEM_URL_CQDG/StructureDefinition/FullSizeExtension"
  val AGE_AT_RECRUITMENT_S_D = s"$SYSTEM_URL_CQDG/StructureDefinition/AgeAtRecruitment"
  // Patient
  val ETHNICITY_S_D = s"$SYSTEM_URL_CQDG/StructureDefinition/QCEthnicity"
  val AGE_OF_DEATH_S_D = s"$SYSTEM_URL_CQDG/StructureDefinition/AgeOfDeath"
  // ResearchStudy
  val ACCESS_LIMITATIONS_S_D = s"$SYSTEM_URL_CQDG/StructureDefinition/AccessLimitations"
  val ACCESS_REQUIREMENTS_S_D = s"$SYSTEM_URL_CQDG/StructureDefinition/AccessRequirements"
  val POPULATION_S_D = s"$SYSTEM_URL_CQDG/StructureDefinition/ResearchStudy/population"
  // Specimen
  val AGE_BIO_COLLECTION_S_D = s"$SYSTEM_URL_CQDG/StructureDefinition/AgeAtBioSpecimenCollection"
}
