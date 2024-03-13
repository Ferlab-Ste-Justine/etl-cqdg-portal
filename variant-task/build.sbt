val projectName = "variant-task"
val organization = "bio.ferlab"
val version = "1.0"
val glowVersion = "2.0.0"
libraryDependencies ++= Seq(
  "io.projectglow" %% "glow-spark3" % glowVersion % Provided exclude ("org.apache.hadoop", "hadoop-client")
)
assembly / mainClass := Some("bio.ferlab.etl.VariantTask")
assembly / assemblyJarName := "variant-task.jar"
