import bio.ferlab.datalake.commons.config.{ConfigurationLoader, SimpleConfiguration}
import pureconfig.generic.auto._

//TODO re-use
trait WithTestConfig {
   implicit lazy val conf: SimpleConfiguration = ConfigurationLoader.loadFromResources[SimpleConfiguration]("config/dev-cqdg.conf")
}
