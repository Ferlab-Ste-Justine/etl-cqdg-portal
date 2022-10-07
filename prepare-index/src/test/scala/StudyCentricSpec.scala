import bio.ferlab.datalake.commons.config.{Configuration, ConfigurationLoader}
import bio.ferlab.fhir.etl.centricTypes.StudyCentric
import model._
import org.apache.spark.sql.DataFrame
import org.scalatest.matchers.should.Matchers
import org.scalatest.flatspec.AnyFlatSpec

class StudyCentricSpec extends AnyFlatSpec with Matchers with WithSparkSession {

  import spark.implicits._

  implicit val conf: Configuration = ConfigurationLoader.loadFromResources("config/dev-cqdg.conf")

  val patient1: PATIENT =  PATIENT()
  val patient2: PATIENT =  PATIENT( `fhir_id` = "PRT0000002",`gender` = "male", `age_at_recruitment` = "215640979761", `submitter_participant_id` = "35849419216")
  val patient3: PATIENT =  PATIENT( `fhir_id` = "PRT0000003",`gender` = "male", `age_at_recruitment` = "215557091632", `ethnicity` = "aboriginal" ,`submitter_participant_id` = "35849430470", `age_of_death` = "3223600")

  val family1: FAMILY_RELATIONSHIP_NEW = FAMILY_RELATIONSHIP_NEW()
  val family2: FAMILY_RELATIONSHIP_NEW = FAMILY_RELATIONSHIP_NEW(`internal_family_relationship_id` = "FAM0000002FR", `submitter_participant_id` = "PRT0000002", `relationship_to_proband` = "Father")
  val family3: FAMILY_RELATIONSHIP_NEW = FAMILY_RELATIONSHIP_NEW(`internal_family_relationship_id` = "FAM0000003FR", `submitter_participant_id` = "PRT0000003", `relationship_to_proband` = "Is the proband")


  "transform" should "prepare index study_centric" in {
    val data: Map[String, DataFrame] = Map(
      "normalized_research_study" -> Seq(RESEARCHSTUDY()).toDF(),
      "normalized_patient" -> Seq(patient1, patient2, patient3).toDF(),
      "normalized_cause_of_death" -> Seq(CAUSE_OF_DEATH()).toDF(),
      "normalized_family_relationship" -> Seq(family1, family2, family3).toDF(),
      "normalized_group" -> Seq(GROUP_NEW()).toDF(),
    )

    val output = new StudyCentric("5", List("STU0000001"))(conf).transform(data)


    output.keys should contain("es_index_study_centric")

    val study_centric = output("es_index_study_centric")
    study_centric.show(false)
    study_centric.printSchema()

    val familyUnitOutput = Seq(
      FAMILY_RELATIONSHIP_OUTPUT(),
      FAMILY_RELATIONSHIP_OUTPUT(`submitter_participant_id` = family2.`submitter_participant_id`, `relationship_to_proband` = family2.`relationship_to_proband`),
      FAMILY_RELATIONSHIP_OUTPUT(`submitter_participant_id` = family3.`submitter_participant_id`, `relationship_to_proband` = family3.`relationship_to_proband`)
    )

    val studyCentricOutput = STUDY_CENTRIC()

    study_centric.as[STUDY_CENTRIC].collect() should contain theSameElementsAs
      Seq(studyCentricOutput)
  }

  "transform" should "prepare inde study_centric with family_data false if no group" in {
    val data: Map[String, DataFrame] = Map(
      "normalized_research_study" -> Seq(RESEARCHSTUDY()).toDF(),
      "normalized_patient" -> Seq(PATIENT(), PATIENT()).toDF(),
      "normalized_document_reference" -> Seq(DOCUMENTREFERENCE(), DOCUMENTREFERENCE(), DOCUMENTREFERENCE()).toDF(),
      "normalized_group" -> Seq[GROUP]().toDF(),
      "normalized_specimen" -> Seq(BIOSPECIMEN()).toDF()
    )

    val output = new StudyCentric("re_000001", List("SD_Z6MWD3H0"))(conf).transform(data)

    output.keys should contain("es_index_study_centric")

    val study_centric = output("es_index_study_centric")
    study_centric.as[STUDY_CENTRIC].collect() should contain theSameElementsAs
      Seq(STUDY_CENTRIC())
  }

}
