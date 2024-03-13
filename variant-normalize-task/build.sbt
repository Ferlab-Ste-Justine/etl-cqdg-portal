val projectName = "variant-normalize-task"
val organization = "bio.ferlab"
val version = "1.0"

val glowVersion = "2.0.0"

libraryDependencies ++= Seq(
  "io.projectglow" %% "glow-spark3" % glowVersion exclude ("org.apache.hadoop", "hadoop-client")
)


assembly / mainClass := Some("bio.ferlab.etl.normalized.RunNormalizedGenomic")
assembly / assemblyJarName := "variant-normalize-task.jar"
