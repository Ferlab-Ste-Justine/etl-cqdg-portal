import sbtassembly.AssemblyPlugin.autoImport.assembly

val sparkVersion = "3.5.1"
val datalakeSpark3Version = "14.0.0"
val deltaCoreVersion = "3.1.0"


lazy val fhavro_export = project in file("fhavro-export")
lazy val publish_task = project in file("publish-task")

val sparkDepsSetting = Seq(
  resolvers ++= Seq(
    "Sonatype OSS Snapshots" at "https://s01.oss.sonatype.org/content/repositories/snapshots",
    "Sonatype OSS Releases" at "https://s01.oss.sonatype.org/content/repositories/releases"
  ),

  libraryDependencies ++= Seq(
    "bio.ferlab" %% "datalake-spark3" % datalakeSpark3Version,
    "bio.ferlab" %% "datalake-test-utils" % datalakeSpark3Version % Test,
    "org.apache.spark" %% "spark-sql" % sparkVersion % Provided,
    "org.apache.spark" %% "spark-hive" % sparkVersion % Provided,
    "org.apache.hadoop" % "hadoop-aws" % "3.3.4" % Provided,
    "io.delta" %% "delta-spark" % deltaCoreVersion % Provided,
    "org.scalatest" %% "scalatest" % "3.2.17" % Test,
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
lazy val variant_task = (project in file("variant-task")).dependsOn(config).settings(commonSettings ++ sparkDepsSetting)

