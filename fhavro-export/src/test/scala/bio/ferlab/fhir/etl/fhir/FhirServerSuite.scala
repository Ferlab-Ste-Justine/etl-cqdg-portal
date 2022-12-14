package bio.ferlab.fhir.etl.fhir

import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, TestSuite}

trait FhirServerSuite extends FhirServer with TestSuite with BeforeAndAfterAll with BeforeAndAfter {

  before {
    loadPatient("James", "Hetfield", "PT-00001", "study:SD_001")
    loadPatient("Corey", "Taylor", "PT-00002", "study:SD_001")
    loadPatient("Jonathan", "Davis", "PT-00003", "study:SD_002")
  }

  after {
  }
}
