import sbtassembly.AssemblyPlugin.autoImport.assembly

val datalakeSpark3Version = "10.2.1"
val deltaCoreVersion = "2.1.1"
val glowVersion = "1.2.1"


lazy val fhavro_export = project in file("fhavro-export")

val sparkDepsSetting = Seq(
  libraryDependencies ++= Seq(
    "bio.ferlab" %% "datalake-spark3" % datalakeSpark3Version,
    "bio.ferlab" %% "datalake-test-utils" % datalakeSpark3Version % Test,
    "org.apache.spark" %% "spark-sql" % "3.1.2" % Provided,
    "org.apache.spark" %% "spark-hive" % "3.3.1" % Provided,
    "org.apache.hadoop" % "hadoop-client" % "3.3.1" % Provided,
    "org.apache.hadoop" % "hadoop-aws" % "3.3.1" % Provided,
    "io.delta" %% "delta-core" % deltaCoreVersion,
    "org.scalatest" %% "scalatest" % "3.2.9" % Test,
  )
)
val commonSettings = Seq(
  scalaVersion := "2.12.16",
  version := "0.1.0-SNAPSHOT",

  Compile / unmanagedResourceDirectories += config.base / "output",

  assembly / assemblyShadeRules := Seq(
    ShadeRule.rename("shapeless.**" -> "shadeshapless.@1").inAll
  ),
  assembly / assemblyMergeStrategy := {
    case "META-INF/io.netty.versions.properties" => MergeStrategy.last
    case "META-INF/native/libnetty_transport_native_epoll_x86_64.so" => MergeStrategy.last
    case "META-INF/DISCLAIMER" => MergeStrategy.last
    case "mozilla/public-suffix-list.txt" => MergeStrategy.last
    case "overview.html" => MergeStrategy.last
    case "git.properties" => MergeStrategy.discard
    case "mime.types" => MergeStrategy.first
    case "module-info.class" => MergeStrategy.first
    case PathList("scala", "annotation", "nowarn.class" | "nowarn$.class") => MergeStrategy.first
    case x =>
      val oldStrategy = (assembly / assemblyMergeStrategy).value
      oldStrategy(x)
  },
  assembly / test := {}

)

lazy val config = (project in file("config")).settings(sparkDepsSetting)

lazy val import_task = (project in file("import-task")).dependsOn(config).settings(commonSettings ++ sparkDepsSetting)
lazy val prepare_index = (project in file("prepare-index")).dependsOn(config).settings(commonSettings ++ sparkDepsSetting)
lazy val index_task = (project in file("index-task")).dependsOn(config).settings(commonSettings ++ sparkDepsSetting)
lazy val publish_task = (project in file("publish-task")).dependsOn(config).settings(commonSettings ++ sparkDepsSetting)
lazy val variant_task = (project in file("variant-task")).dependsOn(config).settings(commonSettings ++ sparkDepsSetting)
