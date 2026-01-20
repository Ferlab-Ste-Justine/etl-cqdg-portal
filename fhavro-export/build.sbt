name := "fhavro-export"
version := "0.0.1"

scalaVersion := "2.13.7"

val awsVersion = "2.16.66"
val slf4jVersion = "1.7.30"
val fhavroVersion = "0.0.11"

libraryDependencies ++= Seq(
  "software.amazon.awssdk" % "s3" % awsVersion,
  "software.amazon.awssdk" % "apache-client" % awsVersion,
  "org.slf4j" % "slf4j-api" % slf4jVersion,
  "org.slf4j" % "slf4j-simple" % slf4jVersion,
  "org.typelevel" %% "cats-core" % "2.3.1",
  "com.typesafe.play" %% "play-json" % "2.9.2",
  "com.github.pureconfig" %% "pureconfig" % "0.15.0",
  "com.softwaremill.sttp.client3" %% "core" % "3.1.0",
  "bio.ferlab" % "fhavro" % fhavroVersion,
  "org.keycloak" % "keycloak-authz-client" % "12.0.3",

  // TEST
  "org.scalatest" %% "scalatest" % "3.2.9" % Test,
  "org.testcontainers" % "localstack" % "1.21.4" % Test,
  "com.dimafeng" %% "testcontainers-scala-scalatest" % "0.38.8" % Test
)

Test / fork := true
Test / envVars := Map("AWS_ACCESS_KEY" -> "access_key", "AWS_SECRET_KEY" -> "secret_key")

resolvers ++= Seq("Sonatype OSS Releases" at "https://s01.oss.sonatype.org/content/repositories/snapshots/")

assembly / assemblyMergeStrategy := {
  case PathList("META-INF", _*) => MergeStrategy.discard
  case _                        => MergeStrategy.first
}

assembly / test := {}
assembly / mainClass := Some("bio.ferlab.fhir.etl.FhavroExport")
assembly / assemblyJarName := "fhavro-export.jar"
