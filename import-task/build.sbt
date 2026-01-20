val projectName = "import-task"
val organization = "bio.ferlab"
val version = "1.0"

val awssdkVersion = "2.16.66"

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-avro" % "3.4.2",
  "org.testcontainers" % "localstack" % "1.21.4" % Test,
  "com.dimafeng" %% "testcontainers-scala-scalatest" % "0.38.8" % Test,
  "software.amazon.awssdk" % "s3" % awssdkVersion % Test
)

Test / fork := true
Test / envVars := Map(
  "AWS_ACCESS_KEY" -> "access_key",
  "AWS_SECRET_KEY" -> "secret_key"
)

assembly / mainClass := Some("bio.ferlab.fhir.etl.ImportTask")
assembly / assemblyJarName := "import-task.jar"
