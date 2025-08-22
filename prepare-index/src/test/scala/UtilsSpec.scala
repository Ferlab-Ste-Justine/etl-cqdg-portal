import bio.ferlab.datalake.spark3.loader.GenericLoader.read
import bio.ferlab.fhir.etl.common.Utils._
import model._
import model.input.{BIOSPECIMEN_INPUT, SAMPLE_INPUT}
import org.apache.spark.sql.functions.{array, col, lit}
import org.apache.spark.sql.types._
import org.apache.spark.sql.{DataFrame, Row, functions}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class UtilsSpec extends AnyFlatSpec with Matchers with WithSparkSession {

  import spark.implicits._

  case class ConditionCoding(code: String, category: String)

  val SCHEMA_CONDITION_CODING = "array<struct<category:string,code:string>>"
  val allHpoTerms: DataFrame = read(getClass.getResource("/hpo_terms.json").toString, "Json", Map(), None, None)
  val allMondoTerms: DataFrame = read(getClass.getResource("/mondo_terms.json").toString, "Json", Map(), None, None)

  val inputPatients: DataFrame = Seq(
    PATIENT(`participant_id` = "P1", `fhir_id` = "P1", `submitter_participant_id` = "S_P1"),
    PATIENT(`participant_id` = "P2", `fhir_id` = "P2", `submitter_participant_id` = "S_P2"),
    PATIENT(`participant_id` = "P3", `fhir_id` = "P3", `submitter_participant_id` = "S_P3"),
    PATIENT(`participant_id` = "P4", `fhir_id` = "P4", `submitter_participant_id` = "S_P4")
  ).toDF()

  "addStudy" should "add studies to participant" in {
    val inputStudies = Seq(RESEARCHSTUDY()).toDF()
    val inputParticipants = Seq(PATIENT()).toDF()

    val output = inputParticipants.addStudy(inputStudies)

    output.collect().sameElements(Seq(PARTICIPANT_CENTRIC()))
  }

  "addFamily" should "add families to patients" in {
    val inputFamilies = Seq(
      GROUP(internal_family_id = "Family1", submitter_family_id = "fam1"),
      GROUP(internal_family_id = "Family2", family_members = Seq("P4"), family_type="solo", submitter_family_id = "fam1"),
    ).toDF()

    val inputFamilyRelationship = Seq(
      FAMILY_RELATIONSHIP(`internal_family_relationship_id` = "FAM_REL1", `category` = "category", `submitter_participant_id` = "P1", `focus_participant_id` = "P2", `relationship_to_proband` = "Father"),
      FAMILY_RELATIONSHIP(`internal_family_relationship_id` = "FAM_REL2", `category` = "category", `submitter_participant_id` = "P2", `focus_participant_id` = "P2", `relationship_to_proband` = "Proband"),
      FAMILY_RELATIONSHIP(`internal_family_relationship_id` = "FAM_REL3", `category` = "category", `submitter_participant_id` = "P3", `focus_participant_id` = "P2", `relationship_to_proband` = "Mother"),
    ).toDF()

    val output = inputPatients.addFamily(inputFamilies, inputFamilyRelationship)

    // Should return probands
    val probands = output.select("participant_id", "is_a_proband").where(col("is_a_proband")).drop("is_a_proband").as[String].collect()
//    probands should contain theSameElementsAs Seq("P2", "P4") //FIXME what happen when a participant does not have family relationship (is he a proband)
    probands should contain theSameElementsAs Seq("P2")

    // Should return mapped family relations per participant
    val patients = output.select("participant_id", "family_relationships").as[(String, Seq[FAMILY_RELATIONSHIP_WITH_FAMILY])].collect()
    val p1 = patients.filter(_._1 == "P1").head
    p1._2 should contain theSameElementsAs Seq(
      FAMILY_RELATIONSHIP_WITH_FAMILY(),
      FAMILY_RELATIONSHIP_WITH_FAMILY(
        `participant_id` = "P2",
        `submitter_participant_id` = "S_P2",
        `relationship_to_proband` = "Proband"),
      FAMILY_RELATIONSHIP_WITH_FAMILY(
        `participant_id` = "P3",
        `submitter_participant_id` = "S_P3",
        `relationship_to_proband` = "Mother"),
    )
  }

  "addCauseOfDeath" should "add cause of death to participant" in {
    val causeOfDeath = Seq(CAUSE_OF_DEATH(`fhir_id` = "COD1", `submitter_participant_ids` = "P1")).toDF()

    val output = inputPatients.addCauseOfDeath(causeOfDeath)
    val participantCauseOfDeath = output.select("participant_id", "cause_of_death").as[(String, String)].collect()

    participantCauseOfDeath.filter(_._1 == "P1").head._2 shouldBe "Pie eating"
  }

  "addSamplesToBiospecimen" should "add multiple samples to biospecimen" in {
    val biospecimensDF = Seq(BIOSPECIMEN_INPUT()).toDF()
    val samplesDF = Seq(
      SAMPLE_INPUT(`fhir_id` = "1"),
      SAMPLE_INPUT(`fhir_id` = "2")
    ).toDF()

    val biospecimenWithSamples = biospecimensDF.addSamplesToBiospecimen(samplesDF)

    biospecimenWithSamples.select("fhir_id", "sample_id").as[(String, String)].collect() should contain theSameElementsAs
      Seq(("BIO0036882","1"), ("BIO0036882","2"))
  }

  "addFilesWithBiospecimen" should "only return participants with files" in {
    val filesDf = Seq(
      FILE_INPUT(`participant_id` = "P1"),
      FILE_INPUT(`participant_id` = "P2"),
      FILE_INPUT(`participant_id` = "P3"),
    ).toDF()
    val biospecimenDF = Seq.empty[BIOSPECIMEN_INPUT].toDF()
    val seqExperimentDF = Seq.empty[SEQUENCING_EXPERIMENT_SINGLE].toDF()
      .withColumn("ldm_sample_id", lit("").cast(StringType))
      .withColumn("lab_aliquot_ids", array().cast(ArrayType(StringType)))
      .withColumn("is_paired_end", lit(false).cast(BooleanType))
    val samplesDF = Seq.empty[SAMPLE_INPUT].toDF()

    val output = inputPatients.addFilesWithBiospecimen(filesDf, biospecimenDF, seqExperimentDF, samplesDF)

    output.select("participant_id").as[String].collect() should not contain "P4"
  }

  "addParticipantWithBiospecimen" should "only return files with participant" in {
    val filesDf = Seq(
      FILE_INPUT(`participant_id` = "P1", `fhir_id` = "F1"),
      FILE_INPUT(`participant_id` = "P2", `fhir_id` = "F2"),
      FILE_INPUT(`participant_id` = "NONE", `fhir_id` = "F3"),
    ).toDF()
    val biospecimenDF = Seq.empty[BIOSPECIMEN_INPUT].toDF()
    val samplesDF = Seq.empty[SAMPLE_INPUT].toDF()

    val output = filesDf.addParticipantWithBiospecimen(inputPatients, biospecimenDF, samplesDF)
    output.select("fhir_id").as[String].collect() should not contain "F3"
  }

  "addSequencingExperiment" should " map the correct type_of_sequencing" in {
    val filesDf = Seq(
      FILE_INPUT(`participant_id` = "P1", `fhir_id` = "F1"),
      FILE_INPUT(`participant_id` = "P2", `fhir_id` = "F2"),
    ).toDF()

    val seqExperiment = Seq(
      SEQUENCING_EXPERIMENT_SINGLE(`analysis_files` = Seq(
        ANALYSIS_FILE("Aligned-reads", "F1"),
      )),
      SEQUENCING_EXPERIMENT_SINGLE(`analysis_files` = Seq(
        ANALYSIS_FILE("snv", "F2"),
      )),
    ).toDF()
      .withColumn("ldm_sample_id", lit("").cast(StringType))
      .withColumn("lab_aliquot_ids", array().cast(ArrayType(StringType)))
      .withColumn("is_paired_end", functions.when(col("analysis_files")(0)("data_type") === "Aligned-reads", lit(true)).otherwise(lit(false)))

    val output = filesDf.addSequencingExperiment(seqExperiment)

    output.select("sequencing_experiment.type_of_sequencing").as[String].collect() should contain theSameElementsAs Seq("Paired Reads", "Unpaired Reads")
  }

  val schema: StructType = StructType(Array(
    StructField("study_id", StringType,true),
    StructField("data_categories_from_files", ArrayType(
      StructType(
        StructField("data_category", StringType, true) ::
          StructField("participant_count", IntegerType, false) :: Nil
      )
    ),true),
  ))

  "combineDataCategoryFromFilesAndStudy" should "add DataCategories listed from Study to Data Categories compiled from files" in {
    // Extra Data categories from files
    val study1Df = Seq(
      RESEARCHSTUDY(`study_id` = "study1", `data_categories` = Seq("data_category1", "data_category2", "data_category3"))
    ).toDF()

    val data1 = Seq(Row("study1", Seq(Row("data_category1", 12), Row("data_category2", 2))))

    val dataCategoriesFromFiles1 = spark.createDataFrame(spark.sparkContext.parallelize(data1), schema)

    val output1 = study1Df.combineDataCategoryFromFilesAndStudy(dataCategoriesFromFiles1)
      .select("data_categories").as[Seq[(String, Option[Int])]].collect().head

    output1 should contain theSameElementsAs Seq(("data_category1", Some(12)), ("data_category2", Some(2)), ("data_category3", None))

    // No extra Data categories from files
    val study2Df = Seq(
      RESEARCHSTUDY(`study_id` = "study1", `data_categories` = Seq("data_category2"))
    ).toDF()

    val data2 = Seq(Row("study1", Seq(Row("data_category2", 12))))

    val dataCategoriesFromFiles2 = spark.createDataFrame(spark.sparkContext.parallelize(data2), schema)

    val output2 = study2Df.combineDataCategoryFromFilesAndStudy(dataCategoriesFromFiles2)
      .select("data_categories").as[Seq[(String, Option[Int])]].collect().head

    output2 should contain theSameElementsAs Seq(("data_category2", Some(12)))
  }
}


