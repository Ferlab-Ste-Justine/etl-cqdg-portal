package bio.ferlab.etl

import bio.ferlab.datalake.commons.config.{ConfigurationLoader, SimpleConfiguration}

trait WithTestConfig {
  lazy val initConf: SimpleConfiguration = ConfigurationLoader.loadFromResources[SimpleConfiguration]("config/dev-cqdg.conf")
  lazy implicit val conf: SimpleConfiguration = initConf
}
