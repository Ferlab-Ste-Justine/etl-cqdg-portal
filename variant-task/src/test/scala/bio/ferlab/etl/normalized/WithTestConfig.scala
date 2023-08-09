package bio.ferlab.etl.normalized

import bio.ferlab.datalake.commons.config.{ConfigurationLoader, SimpleConfiguration}

trait WithTestConfig {
  private val initConf: SimpleConfiguration = ConfigurationLoader.loadFromResources[SimpleConfiguration]("config/test.conf")
  implicit val conf: SimpleConfiguration = initConf
}
