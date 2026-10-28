package bio.ferlab.fhir.etl.minio

import bio.ferlab.fhir.etl.IContainer
import com.dimafeng.testcontainers.GenericContainer

case object MinioContainer extends IContainer {
  val name = "clin-pipeline-minio-test"
  val port = 9000

  // RustFS (S3-compatible) rather than MinIO: MinIO images can no longer be pulled anonymously.
  // The image serves /data on port 9000 by default, so no command is needed.
  val container: GenericContainer = GenericContainer(
    "rustfs/rustfs:1.0.0",
    exposedPorts = Seq(port),
    labels = Map("name" -> name),
    env = Map(
      "RUSTFS_ACCESS_KEY" -> "access_key",
      "RUSTFS_SECRET_KEY" -> "secret_key"
    )
  )
}
