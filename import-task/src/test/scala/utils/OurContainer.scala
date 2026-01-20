package utils

import com.dimafeng.testcontainers.GenericContainer

import scala.jdk.CollectionConverters._

trait OurContainer {
  def container: GenericContainer

  private var isStarted = false

  def name: String

  def port: Int

  private var publicPort: Int = -1
  private var ipAddress: String = "localhost"

  def startIfNotRunning(): (Int, String, Boolean) = {
    if (isStarted) {
      (publicPort, ipAddress, false)
    } else {
      val runningContainer =
        container.dockerClient.listContainersCmd().withLabelFilter(Map("name" -> name).asJava).exec().asScala

      val isNew = runningContainer.toList match {
        case Nil =>
          container.start()
          ipAddress = container.containerInfo.getNetworkSettings.getIpAddress
          publicPort = container.mappedPort(port)
          true
        case List(c) =>
          ipAddress = c.getNetworkSettings.getNetworks.values().asScala.head.getIpAddress
          publicPort = c.ports.collectFirst { case p if p.getPrivatePort == port => p.getPublicPort }.get
          false
      }
      isStarted = true
      (publicPort, ipAddress, isNew)
    }
  }

}
