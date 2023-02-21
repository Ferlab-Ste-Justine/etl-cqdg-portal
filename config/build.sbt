val projectName = "import-task"
val organization = "bio.ferlab"
val version = "1.0"

ThisBuild / scalaVersion := "2.12.14"

val datalakeSpark3Version = "5.3.0"

libraryDependencies ++= Seq(
  "bio.ferlab" %% "datalake-spark3" % datalakeSpark3Version,
  "org.slf4j" % "slf4j-simple" % "1.7.36"
)

assembly / assemblyJarName := "config.jar"
