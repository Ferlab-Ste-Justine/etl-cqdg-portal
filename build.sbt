import sbtassembly.AssemblyPlugin.autoImport.assembly

val sparkDepsSetting = Seq(
  libraryDependencies ++= Seq("bio.ferlab" %% "datalake-spark31" % "0.2.46",
    "org.apache.spark" %% "spark-sql" % "3.1.2",
    "org.apache.spark" %% "spark-hive" % "3.1.2",
    "org.apache.spark" %% "spark-avro" % "2.4.2",
    "org.apache.hadoop" % "hadoop-client" % "3.2.0",
    "org.apache.hadoop" % "hadoop-aws" % "3.2.0",
    "io.delta" %% "delta-core" % "1.0.0",
    "org.scalatest" %% "scalatest" % "3.2.9" % Test
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

lazy val config = project in file("config")
lazy val fhavro_export = project in file("fhavro-export")
lazy val import_task = (project in file("import-task")).settings(commonSettings ++ sparkDepsSetting)
lazy val prepare_index = (project in file("prepare-index")).settings(commonSettings ++ sparkDepsSetting)
lazy val index_task = (project in file("index-task")).settings(commonSettings ++ sparkDepsSetting)
lazy val publish_task = (project in file("publish-task")).settings(commonSettings)
