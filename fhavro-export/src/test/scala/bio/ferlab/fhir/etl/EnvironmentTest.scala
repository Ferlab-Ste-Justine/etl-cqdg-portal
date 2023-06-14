package bio.ferlab.fhir.etl

import bio.ferlab.fhir.etl.model.Environment
import org.scalatest.funsuite.AnyFunSuite

class EnvironmentTest extends AnyFunSuite {

  test("fromString to Environment enum.") {
    assert(Environment.fromString("CQDG-DEV") === Environment.CQDGDEV)
  }

  test("fromString with lowercase string to Environment enum.") {
    assert(Environment.fromString("cqdg-dev") === Environment.CQDGDEV)
  }

  test("fromString with invalid string to Environment enum throws NoSuchElementException") {
    assertThrows[NoSuchElementException](Environment.fromString("TEST"))
  }

  test("toString should be lowercase") {
    assert(Environment.CQDGDEV.toString === "cqdg-dev")
  }
}
