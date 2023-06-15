val projectName = "import-task"
val organization = "bio.ferlab"
val version = "1.0"

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-avro" % "2.4.2"
)

assembly / mainClass := Some("bio.ferlab.fhir.etl.ImportTask")
assembly / assemblyJarName := "import-task.jar"
