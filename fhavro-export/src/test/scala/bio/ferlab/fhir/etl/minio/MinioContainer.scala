package bio.ferlab.fhir.etl.minio

import bio.ferlab.fhir.etl.IContainer
import com.dimafeng.testcontainers.GenericContainer

case object MinioContainer extends IContainer {
  val name = "clin-pipeline-minio-test"
  val port = 9000

  val container: GenericContainer = GenericContainer(
    "minio/minio",
    command = Seq("server", "/data"),
    exposedPorts = Seq(port),
    labels = Map("name" -> name),
    env = Map(
      "MINIO_ACCESS_KEY" -> "access_key",
      "MINIO_SECRET_KEY" -> "secret_key",
      "AWS_REGION" -> "us-east-1"
    )
  )
}
