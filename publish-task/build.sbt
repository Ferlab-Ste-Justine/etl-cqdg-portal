val projectName = "publish-task"
val organization = "bio.ferlab"
val version = "1.0"

libraryDependencies ++= Seq(
  "com.github.pureconfig" %% "pureconfig" % "0.17.6",
  "org.apache.httpcomponents" % "httpclient" % "4.5.14",
  "org.slf4j" % "slf4j-simple" % "1.7.36",
  "com.lihaoyi" %% "mainargs" % "0.7.2"
)

assembly / mainClass := Some("bio.ferlab.fhir.etl.PublishTask")
assembly / assemblyJarName := "publish-task.jar"
