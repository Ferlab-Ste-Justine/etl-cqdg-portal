package conf

import cats.data.ValidatedNel
import cats.implicits.catsSyntaxValidatedId
import pureconfig.ConfigReader.Result
import pureconfig.ConfigSource
import pureconfig.generic.auto._


case class AWSConf(
                    accessKey: String,
                    secretKey: String,
                    endpoint: String,
                    bucketName: String,
                    pathStyleAccess: Boolean,
                    outputBucketName: String,
                    outputPrefix: String,
                    outputNarvalBucket: String
                  )

case class Conf(aws: AWSConf)

object Conf {

  def readConf(): ValidatedNel[String, Conf] = {
    val confResult: Result[Conf] = ConfigSource.default.load[Conf]
    confResult match {
      case Left(errors) =>
        val message = errors.prettyPrint()
        message.invalidNel[Conf]
      case Right(conf) => conf.validNel[String]
    }
  }
}
