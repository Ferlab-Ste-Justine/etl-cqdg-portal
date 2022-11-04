package bio.ferlab.fhir

package object etl {
  //SYSTEM URL
  val SYSTEM_URL_INCLUDE = "https://include.org/htp/fhir"
  val SYSTEM_URL_KF = "https://kf-api-dataservice.kidsfirstdrc.org"
  val SYSTEM_URL = Seq(SYSTEM_URL_INCLUDE, SYSTEM_URL_KF)

  //ResearchStudy
  val ACCESS_LIMITATIONS_URL = "http://fhir.cqdg.ferlab.bio/StructureDefinition/ResearchStudy/limitation"
  val ACCESS_REQUIREMENTS_URL = "http://fhir.cqdg.ferlab.bio/StructureDefinition/ResearchStudy/requirement"
  val POPULATION_URL = "https://fhir.cqdg.ferlab.bio/StructureDefinition/ResearchStudy/population"
  val SYS_NCBI_URL = "https://www.ncbi.nlm.nih.gov/projects/gap/cgi-bin"

  //Patient
  val SYS_DATASERVICE_URL = Seq(s"$SYSTEM_URL_KF/participants", s"$SYSTEM_URL_INCLUDE/patient")
  val SYS_US_CORE_RACE_URL = "http://hl7.org/fhir/us/core/StructureDefinition/us-core-race"
  val SYS_AGE_AT_RECRUITMENT = "https://fhir.cqdg.ferlab.bio/StructureDefinition/ResearchSubject/ageAtRecruitment"
  val SYS_ETHNICITY_URL = "https://fhir.cqdg.ferlab.bio/StructureDefinition/Patient/Ethnicity"
  val SYS_AGE_OF_DEATH_URL = "https://fhir.cqdg.ferlab.bio/StructureDefinition/Patient/age-of-death"

  //DocumentReference
  val DOCUMENT_SIZE = "https://fhir.cqdg.ferlab.bio/StructureDefinition/full-size"

  //Observation
  //TODO same for include and KF?
  val ROLE_CODE_URL = Seq("http://terminology.hl7.org/CodeSystem")

  val URN_UNIQUE_ID = "urn:kids-first:unique-string"

  val SYS_DATA_TYPES = "https://includedcc.org/fhir/code-systems/data_types"
  val SYS_EXP_STRATEGY = "https://includedcc.org/fhir/code-systems/experimental_strategies"
  val SYS_DATA_CATEGORIES = "https://includedcc.org/fhir/code-systems/data_categories"
  val SYS_PROGRAMS = "https://includedcc.org/fhir/code-systems/programs"
  val SYS_DATA_ACCESS_TYPES = "https://includedcc.org/fhir/code-systems/data_access_types"
  val DOCUMENT_DATA_TYPE = "https://fhir.cqdg.ferlab.bio/CodeSystem/data-type"
  val DOCUMENT_DATA_CATEGORY = "https://fhir.cqdg.ferlab.bio/CodeSystem/data-category"
  val SYS_AGE_AT_PHENOTYPE = "http://fhir.cqdg.ferlab.bio/StructureDefinition/Observation/AgeAtPhenotype"

  //Task
  val TASK_BIO_INFO = "https://fhir.cqdg.ferlab.bio/CodeSystem/bioinfo-analysis-code"
  val SEQUENCING_EXPERIMENT = "https://fhir.cqdg.ferlab.bio/StructureDefinition/sequencing-experiment"
  val WORKFLOW = "https://fhir.cqdg.ferlab.bio/StructureDefinition/workflow"

}
