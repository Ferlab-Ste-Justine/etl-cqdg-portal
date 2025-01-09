import bio.ferlab.datalake.commons.config.{Configuration, ConfigurationLoader, ConfigurationWrapper, DatalakeConf, SimpleConfiguration}
import bio.ferlab.fhir.etl.ImportRawToNormalizedETL
import bio.ferlab.fhir.etl.fhavro.FhavroToNormalizedMappings
import models._
import org.apache.spark.sql.SparkSession
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import utils.MinioServer

case class ETLConfiguration(`es-config`: Map[String, String], datalake: DatalakeConf) extends ConfigurationWrapper(datalake)

class ImportTaskSpec extends AnyFlatSpec with Matchers with MinioServer {


  val study = "CAG"

  private def addObjectToBucket(objects: Seq[(String, String)]): Unit = {
    objects.foreach({ case (path, fileName) => transferFromResource(s"fhir/$path/study_id=$study", s"$fileName.avro")})
  }

  implicit val spark: SparkSession = SparkSession.builder()
    .config("spark.ui.enabled", value = false)
    .config("fs.s3a.endpoint", s"http://$apiAddress:9000")
    .config("spark.databricks.delta.merge.repartitionBeforeWrite.enabled", "true")
    .config("spark.databricks.delta.retentionDurationCheck.enabled", "false")
    .config("spark.databricks.delta.schema.autoMerge.enabled", "true")
    .config("spark.delta.merge.repartitionBeforeWrite", "true")
    .config("spark.sql.autoBroadcastJoinThreshold", "-1")
    .config("spark.sql.catalog.spark_catalog", "org.apache.spark.sql.delta.catalog.DeltaCatalog")
    .config("spark.sql.extensions", "io.delta.sql.DeltaSparkSessionExtension")
    .config("spark.sql.legacy.parquet.datetimeRebaseModeInWrite", "CORRECTED")
    .config("spark.sql.legacy.timeParserPolicy", "CORRECTED")
    .config("spark.sql.mapKeyDedupPolicy", "LAST_WIN")
    .config("fs.s3a.path.style.access", "true")
    .master("local")
    .getOrCreate()

  spark.sparkContext.setLogLevel("ERROR")

  "Import Task" should "generate desired normalized outputs" in {
    import spark.implicits._

    assert("access_key" === System.getenv("AWS_ACCESS_KEY"))
    assert("secret_key" === System.getenv("AWS_SECRET_KEY"))

    val objects = Seq(
      ("researchstudy", "cqdg-researchstudy"),
      ("condition", "cqdg-disease"),
      ("documentreference", "cqdg-documentreference"),
      ("group", "cqdg-group"),
      ("observation", "cqdg-observation"),
      ("patient", "cqdg-patient"),
      ("specimen", "cqdg-specimen"),
      ("task", "cqdg-task")
    )

    addObjectToBucket(objects)

    implicit val conf: Configuration = ConfigurationLoader.loadFromResources[SimpleConfiguration]("config/dev-cqdg.conf")

    val jobs = FhavroToNormalizedMappings
      .mappings()
      .map { case (src, dst, transformations) => new ImportRawToNormalizedETL(src, dst, transformations, List(study)) }

//    jobs.foreach(_.run().foreach(e => {
//      ClassGenerator
//        .writeClassFile(
//          "models",
//          e._1.toUpperCase,
//          e._2,
//          "import-task/src/test/scala/")
//    }))

    val dfs = jobs.map(_.run())

    val patients = dfs.flatMap(p => p.find(q => q._1 == "normalized_patient")).head._2.as[NORMALIZED_PATIENT].collect()
    val normalizedDiagnosis = dfs.flatMap(p => p.find(q => q._1 == "normalized_diagnosis")).head._2.as[NORMALIZED_DIAGNOSIS].collect()
    val normalizedDisease_status = dfs.flatMap(p => p.find(q => q._1 == "normalized_disease_status")).head._2.as[NORMALIZED_DISEASE_STATUS].collect()
    val normalizedPhenotype = dfs.flatMap(p => p.find(q => q._1 == "normalized_phenotype")).head._2.as[NORMALIZED_PHENOTYPE].collect()
    val normalizedBiospecimen = dfs.flatMap(p => p.find(q => q._1 == "normalized_biospecimen")).head._2.as[NORMALIZED_BIOSPECIMEN].collect()
    val normalizedSampleRegistration = dfs.flatMap(p => p.find(q => q._1 == "normalized_sample_registration")).head._2.as[NORMALIZED_SAMPLE_REGISTRATION].collect()
    val normalizedResearchStudy = dfs.flatMap(p => p.find(q => q._1 == "normalized_research_study")).head._2.as[NORMALIZED_RESEARCH_STUDY].collect()
    val normalizedDocumentReference = dfs.flatMap(p => p.find(q => q._1 == "normalized_document_reference")).head._2.as[NORMALIZED_DOCUMENT_REFERENCE].collect()
    val group = dfs.flatMap(p => p.find(q => q._1 == "normalized_group")).head._2.as[NORMALIZED_GROUP].collect()
    val task = dfs.flatMap(p => p.find(q => q._1 == "normalized_task")).head._2.as[NORMALIZED_TASK].collect()

    patients.filter(_.`fhir_id` == "PRT0000001").head shouldEqual NORMALIZED_PATIENT()
    normalizedDiagnosis.filter(_.`subject` == "PRT0000001").head shouldEqual NORMALIZED_DIAGNOSIS()
    normalizedDisease_status.filter(_.`fhir_id` == "FAM0000001DS").head shouldEqual NORMALIZED_DISEASE_STATUS()
    normalizedPhenotype.filter(_.`fhir_id` == "PHE0000002").head shouldEqual NORMALIZED_PHENOTYPE()
    normalizedBiospecimen.filter(_.`fhir_id` == "BIO0000001").head shouldEqual NORMALIZED_BIOSPECIMEN()
    normalizedSampleRegistration.filter(_.`subject` == "PRT0000001").head shouldEqual NORMALIZED_SAMPLE_REGISTRATION()
    normalizedResearchStudy.head shouldEqual NORMALIZED_RESEARCH_STUDY()
    normalizedDocumentReference.filter(_.`fhir_id` == "FIL0000212").head shouldBe NORMALIZED_DOCUMENT_REFERENCE()
    group.filter(_.`internal_family_id` == "FM00000001").head shouldEqual NORMALIZED_GROUP()
    task.filter(_.`fhir_id` == "SXP0000001").head shouldEqual NORMALIZED_TASK()
  }
}
