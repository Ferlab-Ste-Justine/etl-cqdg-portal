val projectName = "index-task"
val organization = "bio.ferlab"
val version = "1.0"

libraryDependencies ++= Seq(
  "org.opensearch.client" %% "opensearch-spark-30" % "1.0.0"
)

assembly / mainClass := Some("bio.ferlab.fhir.etl.IndexTask")
assembly / assemblyJarName := "index-task.jar"
