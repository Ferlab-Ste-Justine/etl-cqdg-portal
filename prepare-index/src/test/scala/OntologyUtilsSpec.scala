import bio.ferlab.datalake.spark3.loader.GenericLoader.read
import bio.ferlab.fhir.etl.common.OntologyUtils.getTaggedPhenotypes
import model.{PHENOTYPE, PHENOTYPE_HPO_CODE, PHENOTYPE_TAGGED, PHENOTYPE_TAGGED_WITH_ANCESTORS}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class OntologyUtilsSpec extends AnyFlatSpec with Matchers with WithSparkSession {

  case class ConditionCoding(code: String, category: String)
  import spark.implicits._


  "getTaggedPhenotypes" should "return tagged phenotypes and tagged phenotypes with ancestors" in {

    val phenotype1 = PHENOTYPE(`fhir_id` = "1", `phenotype_HPO_code` = PHENOTYPE_HPO_CODE(`code` = "HP:G"), `cqdg_participant_id` = "1")
    val phenotype2 = PHENOTYPE(`fhir_id` = "2", `phenotype_HPO_code` = PHENOTYPE_HPO_CODE(`code` = "HP:E"), `cqdg_participant_id` = "1")
    val phenotype3 = PHENOTYPE(`fhir_id` = "3", `phenotype_HPO_code` = PHENOTYPE_HPO_CODE(`code` = "HP:C"), `cqdg_participant_id` = "1", `phenotype_observed` = "NEG")

    val hpo_terms = read(getClass.getResource("/ontology_terms_test.json").toString, "Json", Map(), None, None)

    val phenotypes = Seq(phenotype1, phenotype2, phenotype3).toDF()

    val (t1, t2, t3) = getTaggedPhenotypes(phenotypes, hpo_terms)

    //observed phenotypes tagged
    val taggedPhenotypes = t1.as[(String, Seq[PHENOTYPE_TAGGED])].collect().head

    taggedPhenotypes shouldBe
      ("1", Seq(
        PHENOTYPE_TAGGED(`internal_phenotype_id` = "1", `phenotype_id` = "HP:G", `is_leaf` = true, `name` = "G Name", `parents` = Seq("B Name (HP:B)"), `age_at_event` = 0, `display_name` = "G Name (HP:G)"),
        PHENOTYPE_TAGGED(`internal_phenotype_id` = "2", `phenotype_id` = "HP:E", `is_leaf` = true, `name` = "E Name", `parents` = Seq("B Name (HP:B)", "C Name (HP:C)"), `age_at_event` = 0, `display_name` = "E Name (HP:E)"),
      ))

    //observed phenotypes tagged
    val notTaggedPhenotypes = t2.as[(String, Seq[PHENOTYPE_TAGGED])].collect().head

    notTaggedPhenotypes shouldBe
      ("1", Seq(
        PHENOTYPE_TAGGED(`internal_phenotype_id` = "3", `phenotype_id` = "HP:C", `name` = "C Name", `parents` = Seq("A Name (HP:A)"), `display_name` = "C Name (HP:C)"),
      ))

    //observed phenotypes with ancestors
    val taggedPhenotypesWithAncestors = t3.as[(String, Seq[PHENOTYPE_TAGGED_WITH_ANCESTORS])].collect().head

    taggedPhenotypesWithAncestors._2 should contain theSameElementsAs
      Seq(
        PHENOTYPE_TAGGED_WITH_ANCESTORS(`phenotype_id` = "HP:C", `name` = "C Name", `parents` = Seq("A Name (HP:A)"), `age_at_event` = Seq(0), `display_name` = "C Name (HP:C)"),
        PHENOTYPE_TAGGED_WITH_ANCESTORS(`phenotype_id` = "HP:E", `is_leaf` = true, `is_tagged` = true,`name` = "E Name", `parents` = Seq("B Name (HP:B)", "C Name (HP:C)"), `age_at_event` = Seq(0), `display_name` = "E Name (HP:E)"),
        PHENOTYPE_TAGGED_WITH_ANCESTORS(`phenotype_id` = "HP:G", `is_leaf` = true, `is_tagged` = true,`name` = "G Name", `parents` = Seq("B Name (HP:B)"), `age_at_event` = Seq(0), `display_name` = "G Name (HP:G)"),
        PHENOTYPE_TAGGED_WITH_ANCESTORS(`phenotype_id` = "HP:B", `name` = "B Name", `parents` = Seq("A Name (HP:A)"), `age_at_event` = Seq(0), `display_name` = "B Name (HP:B)"),
        PHENOTYPE_TAGGED_WITH_ANCESTORS(`phenotype_id` = "HP:A", `name` = "A Name", `parents` = Nil, `age_at_event` = Seq(0), `display_name` = "A Name (HP:A)"),
      )
  }

}
