package bio.ferlab.fhir.etl

import bio.ferlab.fhir.Fhavro
import bio.ferlab.fhir.etl.config.FhirRequest
import bio.ferlab.fhir.etl.fhir.FhirServerSuite
import bio.ferlab.fhir.etl.minio.MinioServerSuite
import bio.ferlab.fhir.etl.task.FhavroExporter
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class FhavroExporterTest extends AnyFlatSpec with FhirServerSuite with MinioServerSuite with Matchers {

  "requestExportFor" should "return a List of Fhir Resource by tag" in {
    val fhirRequest = FhirRequest("Patient", "cqdg-patient", None, None, None, None, None)
    val resources = new FhavroExporter("input", "SD_001").requestExportFor(fhirRequest)
    resources.length shouldBe 2
  }

  it should "return a List of Fhir Resource by query param" in {
    loadCondition(code = "1", tag = "study:SD_001")
    loadCondition(code = "2", tag = "study:SD_001")
    loadCondition(code = "3", tag = "study:SD_001")
    loadCondition(system = "https://nih-ncpi.github.io/ncpi-fhir-ig/data-dictionary/SD_7YDC1W4H/condition_code", code = "4", tag = "study:SD_001")
    val fhirRequest = FhirRequest("Condition", "cqdg-condition", None, None, None, None, Some(Map("code" -> List("http://purl.obolibrary.org/obo/mondo.owl|"))))
    val resources = new FhavroExporter("input", "SD_001").requestExportFor(fhirRequest)
    resources.length shouldBe 3
  }

  "convertFileContentToGenericRecord" should "return a list of Generic Record" in {
    val fhirRequest = FhirRequest("Patient", "cqdg-patient", None, None, None, None, None)
    val fhavroExporter = new FhavroExporter("input", "SD_001")
    val resources = fhavroExporter.requestExportFor(fhirRequest)
    val schema = Fhavro.loadSchemaFromResources("schema/patient.avsc")
    fhavroExporter.convertResourcesToGenericRecords(schema, resources).length shouldBe 2
  }
}
