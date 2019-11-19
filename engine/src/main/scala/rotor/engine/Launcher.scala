package rotor.engine

import java.net.InetAddress

import akka.actor.ActorSystem
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession
import org.apache.carbondata.core.constants.CarbonCommonConstants
import org.apache.carbondata.core.util.CarbonProperties
import org.apache.curator.framework.recipes.locks.InterProcessMutex
import rotor.common.Logging
import rotor.common.utils.Zookeeper
import rotor.engine.common.RotorParams
import rotor.engine.core.{EngineActor, RotorSession}


/**
  * Created by iodone on {19-3-4}.
  */
object Launcher extends Logging {


  //Generate legal akka remote address, and reigster to Zookeeper path like this: /rotor/engines/192.168.22.22:30000
  def genNewAddress(oldAdress: List[String]): (String, String) = {
    val orginPort = 30000
    val ip = InetAddress.getLocalHost.getHostAddress
    val port = oldAdress.filter(_.startsWith(ip)).sorted.foldLeft(orginPort) {
      (acc, addr) =>
        if (addr.split(":")(1).toInt == acc) acc + 1
        else acc
    }
    (ip, port.toString)
  }

  def createEngine (rotorSession: RotorSession): SparkSession = {
    val sparkConf = new SparkConf
    rotorSession.engineConf.store.filter(_._1.startsWith("spark.")).foreach {
      s => sparkConf.set(s._1, s._2)
    }

    val engineBuilder = SparkSession.builder().config(sparkConf)
    val engine = engineBuilder.appName(rotorSession.engineConf.getOrElse("rotor.name", "rotor"))
      .enableHiveSupport
      .getOrCreate

    engine.sparkContext.setLogLevel("WARN")
    engine
  }

  protected def buildNewAkkaConf(appConf:Config): Option[Config] = {

    // Init zookeeper client
    val zkServers = appConf.getString("zookeeper.servers").split(",").map(_.trim)
    val zkPrefixPath = appConf.getString("zookeeper.prefix")
    val zkClient = Zookeeper.getOrCreate(zkServers, zkPrefixPath)

    // lock
    val mutex = new InterProcessMutex(zkClient.getCuratorFramework, zkPrefixPath + "/engine_lock")

    mutex.acquire()
    val enginesPath = zkClient.listChildPath("/engines")
    val ipAndPort = genNewAddress(enginesPath)

    zkClient.write("/jobs", "")
    val conf = zkClient.write(s"/engines/${ipAndPort._1}:${ipAndPort._2}", System.currentTimeMillis.toString) match {
      case Right(_) => {
        val conf = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + ipAndPort._2)
         .withFallback(ConfigFactory.parseString("akka.actor.provider=\"akka.remote.RemoteActorRefProvider\""))
         .withFallback(ConfigFactory.parseString("akka.remote.netty.tcp.hostname=" + ipAndPort._1))
         .withFallback(appConf)
        Some(conf)
      }
      case Left(errMsg) => {
        logger.fatal(s"Register akka remote addr to zookeeper failed, reason for ${errMsg}")
        None
      }
    }

    mutex.release()
    conf
  }

  def engineShowDownHook(engineAppConf: Config) = {
    sys.addShutdownHook {
      logger.fatal("Engine was shutshown !!!")
      val zkClient = Zookeeper.getOrCreate
      val enginePath = s"""/engines/${engineAppConf.getString("akka.remote.netty.tcp.hostname")}:${engineAppConf.getString("akka.remote.netty.tcp.port")}"""
      zkClient.deleteAll(enginePath)
      zkClient.close
    }
  }

  def main(args: Array[String]) = {

    logger.info("Rotor engine starting ......")

    val engineParams = RotorParams(args.toList)
    buildNewAkkaConf(ConfigFactory.load) match {
      case Some(conf) => {
        val rs = new RotorSession(engineParams, conf)
        val actorSystem = ActorSystem("RotorSystem", conf)
        val engineWorkers = engineParams.getOrElse("rotor.engine.worker.parallelism", "1").toInt
        (1 to engineWorkers).foreach {
          id => actorSystem.actorOf(EngineActor.props(rs), name = id.toString)
        }

        engineShowDownHook(conf)
        logger.info(s"Rotor engine started successfully and ${engineWorkers} workers for service.")

        // TODO: need waitTermination ???
      }
      case _ => logger.fatal("Build new akka conf faild, engine will quit.")
    }
  }
}
