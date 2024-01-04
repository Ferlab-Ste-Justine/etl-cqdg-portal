import bio.ferlab.datalake.spark3.loader.GenericLoader.read
import bio.ferlab.fhir.etl.common.OntologyUtils.{getDiagnosis, getTaggedPhenotypes}
import model.{DIAGNOSIS_INPUT, PHENOTYPE, PHENOTYPE_HPO_CODE, PHENOTYPE_TAGGED, PHENOTYPE_TAGGED_WITH_ANCESTORS, PHENOTYPE_TAGGED_WITH_OBSERVED}
import org.apache.spark.sql.DataFrame
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class OntologyUtilsSpec extends AnyFlatSpec with Matchers with WithSparkSession {

  case class ConditionCoding(code: String, category: String)
  import spark.implicits._

  val hpo_terms: DataFrame = read(getClass.getResource("/ontology_terms_test.json").toString, "Json", Map(), None, None)

  "getTaggedPhenotypes" should "return tagged phenotypes and tagged phenotypes with ancestors" in {

    val phenotype1 = PHENOTYPE(`fhir_id` = "1", `phenotype_source_text` = "text", `phenotype_HPO_code` = PHENOTYPE_HPO_CODE(`code` = "HP:G"), `cqdg_participant_id` = "1", `age_at_phenotype` = Some("Young"))
    val phenotype2 = PHENOTYPE(`fhir_id` = "2", `phenotype_source_text` = "text", `phenotype_HPO_code` = PHENOTYPE_HPO_CODE(`code` = "HP:E"), `cqdg_participant_id` = "1", `age_at_phenotype` = Some("Old"))
    val phenotype3 = PHENOTYPE(`fhir_id` = "3", `phenotype_source_text` = "text", `phenotype_HPO_code` = PHENOTYPE_HPO_CODE(`code` = "HP:C"), `cqdg_participant_id` = "1", `phenotype_observed` = "NEG", `age_at_phenotype` = Some("Super Old"))

    val phenotypes = Seq(phenotype1, phenotype2, phenotype3).toDF()

    val (_, t2, t3) = getTaggedPhenotypes(phenotypes, hpo_terms)

    //phenotypes tagged
    val taggedPhenotypes = t3.as[(String, Seq[PHENOTYPE_TAGGED_WITH_OBSERVED])].collect().head

    taggedPhenotypes shouldBe
      ("1", Seq(
        PHENOTYPE_TAGGED_WITH_OBSERVED(`internal_phenotype_id` = "1", `is_leaf` = true, `name` = "G Name (HP:G)", `parents` = Seq("B Name (HP:B)"), `age_at_event` = Some("Young"), `source_text` = "text", `is_observed` = Some(true)),
        PHENOTYPE_TAGGED_WITH_OBSERVED(`internal_phenotype_id` = "2", `is_leaf` = true, `name` = "E Name (HP:E)", `parents` = Seq("B Name (HP:B)", "C Name (HP:C)"), `age_at_event` = Some("Old"), `source_text` = "text", `is_observed` = Some(true)),
        PHENOTYPE_TAGGED_WITH_OBSERVED(`internal_phenotype_id` = "3", `parents` = Seq("A Name (HP:A)"), `name` = "C Name (HP:C)", `age_at_event` = Some("Super Old"), `source_text` = "text", `is_observed` = Some(false)),
      ))

    //observed phenotypes with ancestors
    val taggedPhenotypesWithAncestors = t2.as[(String, Seq[PHENOTYPE_TAGGED_WITH_ANCESTORS])].collect().head

    taggedPhenotypesWithAncestors._2 should contain theSameElementsAs
      Seq(
        PHENOTYPE_TAGGED_WITH_ANCESTORS(`parents` = Seq("A Name (HP:A)"), `age_at_event` = Seq("Old"), `name` = "C Name (HP:C)"),
        PHENOTYPE_TAGGED_WITH_ANCESTORS(`is_leaf` = true, `is_tagged` = true, `parents` = Seq("B Name (HP:B)", "C Name (HP:C)"), `age_at_event` = Seq("Old"), `name` = "E Name (HP:E)"),
        PHENOTYPE_TAGGED_WITH_ANCESTORS(`is_leaf` = true, `is_tagged` = true, `parents` = Seq("B Name (HP:B)"), `age_at_event` = Seq("Young"), `name` = "G Name (HP:G)"),
        PHENOTYPE_TAGGED_WITH_ANCESTORS(`parents` = Seq("A Name (HP:A)"), `age_at_event` = Seq("Old", "Young"), `name` = "B Name (HP:B)"),
        PHENOTYPE_TAGGED_WITH_ANCESTORS(`parents` = Nil, `age_at_event` = Seq( "Old", "Young"), `name` = "A Name (HP:A)"),
      )
  }

  it should "return replace phenotypes that are obsolete" in {
    val phenotype1 = PHENOTYPE(`fhir_id` = "1", `phenotype_HPO_code` = PHENOTYPE_HPO_CODE(`code` = "HP:E"), `cqdg_participant_id` = "1", `age_at_phenotype` = Some("Young"), `phenotype_source_text` = "text")
    val phenotype2 = PHENOTYPE(`fhir_id` = "2", `phenotype_HPO_code` = PHENOTYPE_HPO_CODE(`code` = "HP:D"), `cqdg_participant_id` = "1", `phenotype_source_text` = "text")

    val phenotypes = Seq(phenotype1, phenotype2).toDF()

    val (_, t2, t3) = getTaggedPhenotypes(phenotypes, hpo_terms)

    //observed phenotypes tagged
    val taggedPhenotypes = t3.as[(String, Seq[PHENOTYPE_TAGGED_WITH_OBSERVED])].collect().head

    taggedPhenotypes shouldBe
      ("1", Seq(
        PHENOTYPE_TAGGED_WITH_OBSERVED(`internal_phenotype_id` = "1", `is_leaf` = true, `name` = "E Name (HP:E)", `parents` = Seq("B Name (HP:B)", "C Name (HP:C)"), `age_at_event` = Some("Young"), `source_text` = "text", `is_observed` = Some(true)),
        // HP:D (obsolete) should be changed to HP:G (alternate)
        PHENOTYPE_TAGGED_WITH_OBSERVED(`internal_phenotype_id` = "2", `is_leaf` = true, `name` = "G Name (HP:G)", `parents` = Seq("B Name (HP:B)"), `age_at_event` = None, `source_text` = "text", `is_observed` = Some(true)),
      ))

    //observed phenotypes with ancestors
    val taggedPhenotypesWithAncestors = t2.as[(String, Seq[PHENOTYPE_TAGGED_WITH_ANCESTORS])].collect().head

    taggedPhenotypesWithAncestors._2 should contain theSameElementsAs
      Seq(
        PHENOTYPE_TAGGED_WITH_ANCESTORS(`parents` = Seq("A Name (HP:A)"), `age_at_event` = Seq("Young"), `name` = "C Name (HP:C)"),
        PHENOTYPE_TAGGED_WITH_ANCESTORS(`is_leaf` = true, `is_tagged` = true, `parents` = Seq("B Name (HP:B)", "C Name (HP:C)"), `age_at_event` = Seq("Young"), `name` = "E Name (HP:E)"),
        PHENOTYPE_TAGGED_WITH_ANCESTORS(`is_leaf` = true, `is_tagged` = true, `parents` = Seq("B Name (HP:B)"), `age_at_event` = Nil, `name` = "G Name (HP:G)"),
        PHENOTYPE_TAGGED_WITH_ANCESTORS(`parents` = Seq("A Name (HP:A)"), `age_at_event` = Seq("Young"), `name` = "B Name (HP:B)"),
        PHENOTYPE_TAGGED_WITH_ANCESTORS(`parents` = Nil, `age_at_event` = Seq("Young"), `name` = "A Name (HP:A)"),
      )
  }

  it should "return phenotypes when no age at phenotype present" in {
    val phenotype1 = PHENOTYPE(`fhir_id` = "1", `phenotype_HPO_code` = PHENOTYPE_HPO_CODE(`code` = "HP:G"), `cqdg_participant_id` = "1", `age_at_phenotype` = Some("Young"), `phenotype_source_text` = "text")
    val phenotype2 = PHENOTYPE(`fhir_id` = "2", `phenotype_HPO_code` = PHENOTYPE_HPO_CODE(`code` = "HP:E"), `cqdg_participant_id` = "1", `phenotype_source_text` = "text")

    val phenotypes = Seq(phenotype1, phenotype2).toDF()

    val (_, t2, t3) = getTaggedPhenotypes(phenotypes, hpo_terms)

    //observed phenotypes tagged
    val taggedPhenotypes = t3.as[(String, Seq[PHENOTYPE_TAGGED_WITH_OBSERVED])].collect().head

    taggedPhenotypes shouldBe
      ("1", Seq(
        PHENOTYPE_TAGGED_WITH_OBSERVED(`internal_phenotype_id` = "1", `is_leaf` = true, `name` = "G Name (HP:G)", `parents` = Seq("B Name (HP:B)"), `age_at_event` = Some("Young"), `source_text` = "text", `is_observed` = Some(true)),
        PHENOTYPE_TAGGED_WITH_OBSERVED(`internal_phenotype_id` = "2", `is_leaf` = true, `name` = "E Name (HP:E)", `parents` = Seq("B Name (HP:B)", "C Name (HP:C)"), `age_at_event` = None, `source_text` = "text", `is_observed` = Some(true)),
      ))

    //observed phenotypes with ancestors
    val taggedPhenotypesWithAncestors = t2.as[(String, Seq[PHENOTYPE_TAGGED_WITH_ANCESTORS])].collect().head

    taggedPhenotypesWithAncestors._2 should contain theSameElementsAs
      Seq(
        PHENOTYPE_TAGGED_WITH_ANCESTORS(`parents` = Seq("A Name (HP:A)"), `age_at_event` = Nil, `name` = "C Name (HP:C)"),
        PHENOTYPE_TAGGED_WITH_ANCESTORS(`is_leaf` = true, `is_tagged` = true, `parents` = Seq("B Name (HP:B)", "C Name (HP:C)"), `age_at_event` = Nil, `name` = "E Name (HP:E)"),
        PHENOTYPE_TAGGED_WITH_ANCESTORS(`is_leaf` = true, `is_tagged` = true, `parents` = Seq("B Name (HP:B)"), `age_at_event` = Seq("Young"), `name` = "G Name (HP:G)"),
        PHENOTYPE_TAGGED_WITH_ANCESTORS(`parents` = Seq("A Name (HP:A)"), `age_at_event` = Seq("Young"), `name` = "B Name (HP:B)"),
        PHENOTYPE_TAGGED_WITH_ANCESTORS(`parents` = Nil, `age_at_event` = Seq("Young"), `name` = "A Name (HP:A)"),
      )
  }

  "getDiagnosis" should "return diagnosis per participant" in {

    val diagnosis1 = DIAGNOSIS_INPUT(`subject` = "PRT0000001", `fhir_id` = "DIA0000001", `diagnosis_source_text` = "text1", `diagnosis_mondo_code` = "HP:G", `diagnosis_ICD_code` = "HP:G")
    val diagnosis2 = DIAGNOSIS_INPUT(`subject` = "PRT0000002", `fhir_id` = "DIA0000002", `diagnosis_source_text` = "text2", `diagnosis_mondo_code` = "HP:E", `diagnosis_ICD_code` = "HP:E")
    val diagnosis3 = DIAGNOSIS_INPUT(`subject` = "PRT0000001", `fhir_id` = "DIA0000003", `diagnosis_source_text` = "text3", `diagnosis_mondo_code` = "HP:B", `diagnosis_ICD_code` = "HP:B")

    val terms = read(getClass.getResource("/ontology_terms_test.json").toString, "Json", Map(), None, None)

    val diagnoses = Seq(diagnosis1, diagnosis2, diagnosis3).toDF()

    val (d1, d2) = getDiagnosis(diagnoses, terms, terms)

    //should be grouped per participant
    d1.count() shouldEqual 2
    d2.count() shouldEqual 2

    val result = d1.drop("diagnoses").as[(String, Seq[PHENOTYPE_TAGGED], Seq[PHENOTYPE_TAGGED])].collect()
    val resultP1 = result.filter(_._1 == "PRT0000001").head
    val resultP2 = result.filter(_._1 == "PRT0000002").head

    //should map tagged ICD
    resultP1._2.map(_.`name`) should contain theSameElementsAs Seq("G Name (HP:G)", "B Name (HP:B)")
    resultP2._2.map(_.`name`) should contain theSameElementsAs Seq("E Name (HP:E)")

    //should map tagged MONDO
    resultP1._3.map(_.`name`) should contain theSameElementsAs Seq("G Name (HP:G)", "B Name (HP:B)")
    resultP2._3.map(_.`name`) should contain theSameElementsAs Seq("E Name (HP:E)")
  }

  "getDiagnosis" should "map ICD terms with '.' or '-' inside" in {

    val diagnosis1 = DIAGNOSIS_INPUT(`fhir_id` = "DIA0000001", `diagnosis_ICD_code` = "A28.9")
    val diagnosis2 = DIAGNOSIS_INPUT(`fhir_id` = "DIA0000002", `diagnosis_ICD_code` = "A20-A28") //as A20-A28 in input file

    val terms = read(getClass.getResource("/ontology_terms_test.json").toString, "Json", Map(), None, None)

    val diagnoses = Seq(diagnosis1, diagnosis2).toDF()

    val (d1, _) = getDiagnosis(diagnoses, terms, terms)

    val result = d1.select("cqdg_participant_id", "icd_tagged").as[(String, Seq[PHENOTYPE_TAGGED])].collect()
    val resultP1 = result.filter(_._1 == "PRT0000001").head

    resultP1._2.map(_.`name`) should contain theSameElementsAs Seq("ICD 1 (A28.9)", "ICD 2 (A20-A28)")
  }

  it should "return phenotypes tagged observed and non observed" in {
    val phenotype1 = PHENOTYPE(`fhir_id` = "1", `phenotype_source_text` = "text", `phenotype_HPO_code` = PHENOTYPE_HPO_CODE(`code` = "HP:G"), `cqdg_participant_id` = "1", `age_at_phenotype` = Some("Young"))
    val phenotype2 = PHENOTYPE(`fhir_id` = "2", `phenotype_source_text` = "text", `phenotype_HPO_code` = PHENOTYPE_HPO_CODE(`code` = "HP:E"), `cqdg_participant_id` = "1", `age_at_phenotype` = Some("Old"))
    val phenotype3 = PHENOTYPE(`fhir_id` = "3", `phenotype_source_text` = "text", `phenotype_HPO_code` = PHENOTYPE_HPO_CODE(`code` = "HP:C"), `cqdg_participant_id` = "1", `phenotype_observed` = "NEG", `age_at_phenotype` = Some("Old"))

    val phenotypes = Seq(phenotype1, phenotype2, phenotype3).toDF()

    val (_, _, t3) = getTaggedPhenotypes(phenotypes, hpo_terms)
    val taggedPhenotypes = t3.as[(String, Seq[PHENOTYPE_TAGGED_WITH_OBSERVED])].collect()

    taggedPhenotypes.flatMap(e => e._2.map(p => (p.`internal_phenotype_id`, p.`is_observed`)))

    taggedPhenotypes.flatMap(e => e._2.map(p => p.`internal_phenotype_id`-> p.`is_observed`)) shouldBe
      Seq("1" -> Some(true), "2" -> Some(true), "3" -> Some(false))
  }

}
