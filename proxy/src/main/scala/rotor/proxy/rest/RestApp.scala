package rotor.proxy.rest

/**
  * Created by iodone on {18-5-10}.
  */

import akka.actor.ActorSystem
import rotor.common.Config


object RestApp {

  def main(args: Array[String]): Unit = {
    val actorSystem = ActorSystem("Rotor-Proxy")
    val conf = Config.conf
    RestServer(conf.http, actorSystem).start
  }

}

