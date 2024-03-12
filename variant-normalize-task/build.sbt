val projectName = "variant-normalize-task"
val organization = "bio.ferlab"
val version = "1.0"

val sparkVersion = "3.3.2"
val datalakeSpark3Version = "13.0.0"
val deltaCoreVersion = "2.3.0"
val glowVersion = "1.2.1"

libraryDependencies ++= Seq(
  "bio.ferlab" %% "datalake-spark3" % datalakeSpark3Version,
  "bio.ferlab" %% "datalake-test-utils" % datalakeSpark3Version % Test,
  "org.apache.spark" %% "spark-sql" % sparkVersion % Provided,
  "org.apache.spark" %% "spark-hive" % sparkVersion % Provided,
  "org.apache.hadoop" % "hadoop-aws" % "3.3.6" % Provided,
  "io.delta" %% "delta-core" % deltaCoreVersion,
  "io.projectglow" %% "glow-spark3" % glowVersion exclude ("org.apache.hadoop", "hadoop-client") ,
  "org.scalatest" %% "scalatest" % "3.2.9" % Test,
)


assembly / mainClass := Some("bio.ferlab.etl.normalized.RunNormalizedGenomic")
assembly / assemblyJarName := "variant-normalize-task.jar"
