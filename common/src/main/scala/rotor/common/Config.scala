package rotor.common

/**
  * Created by iodone on {18-11-1}.
  */

import pureconfig.loadConfig


case class Akka()
case class HttpConfig(host: String, port: Int)
case class ZookeeperConfig(servers: String, prefix: String)
case class EngineConfig(resultPrefix: String)
case class Config(akka: Akka, http: HttpConfig, zookeeper: ZookeeperConfig, engine: EngineConfig)

object Config {

  val conf = loadConfig[Config] match {
      case Right(config) => config
      case Left(error) =>
        throw new RuntimeException(s"Cannot read config file, errors: \n ${error.toList.mkString("\n")}")
    }
}
