package bio.ferlab.fhir.etl

import bio.ferlab.fhir.etl.fhir.FhirUtils.buildFhirClient
import bio.ferlab.fhir.etl.s3.S3Utils.buildS3Client
import bio.ferlab.fhir.etl.task.FhavroExporter
import ca.uhn.fhir.rest.client.impl.GenericClient
import cats.implicits.catsSyntaxValidatedId
import software.amazon.awssdk.services.s3.S3Client

object FhavroExport extends App {
  println(s"ARGS: " + args.mkString("[", ", ", "]"))

  val Array(studyIds, env) = args

  val studyList = studyIds.split(",").toList

  studyList.foreach(studyId => {
    withSystemExit {
      withLog {
        withConfiguration(env) { configuration =>
          implicit val s3Client: S3Client = buildS3Client(configuration)
          implicit val fhirClient: GenericClient = buildFhirClient(configuration)

          val fhavroExporter = new FhavroExporter(configuration.awsConfig.bucketName, studyId)

          configuration.fhirConfig.resources.foreach { fhirRequest =>
            val resources = fhavroExporter.requestExportFor(fhirRequest)
            fhavroExporter.uploadFiles(fhirRequest, resources)
          }

          "Success!".validNel[String]
        }
      }
    }
  })
}
