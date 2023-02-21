package bio.ferlab.fhir.etl.config

import bio.ferlab.datalake.commons.config.{ConfigurationWrapper, DatalakeConf}
case class ETLConfiguration(`es-config`: Map[String, String], datalake: DatalakeConf) extends ConfigurationWrapper(datalake)

