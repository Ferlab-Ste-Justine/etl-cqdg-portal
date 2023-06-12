package bio.ferlab.etl.normalize

import bio.ferlab.datalake.commons.config.{Configuration, ConfigurationLoader, SimpleConfiguration}
import pureconfig.generic.auto._

//TODO re-use
trait WithTestConfig {
   lazy val initConf: SimpleConfiguration = ConfigurationLoader.loadFromResources[SimpleConfiguration]("config/test.conf")
   lazy implicit val conf: Configuration = initConf
}
