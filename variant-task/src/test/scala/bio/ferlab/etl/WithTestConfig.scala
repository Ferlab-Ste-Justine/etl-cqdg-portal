package bio.ferlab.etl

import bio.ferlab.datalake.commons.config.{ConfigurationLoader, SimpleConfiguration}

trait WithTestConfig {
  private val initConf: SimpleConfiguration = ConfigurationLoader.loadFromResources[SimpleConfiguration]("config/dev-cqdg.conf")
  implicit val conf: SimpleConfiguration = initConf
}
