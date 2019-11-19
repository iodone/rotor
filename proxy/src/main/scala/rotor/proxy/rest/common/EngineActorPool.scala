package rotor.proxy.rest.common

/**
  * Created by iodone on {19-4-11}.
  */

import java.util.concurrent.ConcurrentHashMap

import scala.util.Random
import com.typesafe.scalalogging.LazyLogging

import rotor.common.domain.Entiy.JobState
import rotor.common.utils.Zookeeper
import rotor.common.Config


object EngineActorPool extends LazyLogging {


  case class EngineActor(remoteAddr: String, state: String, path: String)

  val selectedActors = new ConcurrentHashMap[String, EngineActor]()

  def selectIdleActor: Option[EngineActor] = synchronized {
    val idleActors = getAllActors.filter { x =>
      (x.state == JobState.SUCCEEDED || x.state == JobState.KILLED || x.state == JobState.FAILED || x.state == "IDLE") && !selectedActors.contains(x.path)
    }

    val s = idleActors match {
      case Nil => None
      case _ => idleActors.lift(Random.nextInt(idleActors.size))
    }

    if (s.isDefined) selectedActors.put(s.get.path, s.get)
    s
  }

  def giveBackActor(engineActor: EngineActor) = {
    selectedActors.remove(engineActor.path)
  }

  // TODO: Using watch path to replace scan all path
  protected def getAllActors: List[EngineActor] = {
    val zkClient = Zookeeper.getOrCreate()

    val enginesPath = zkClient.listChildPath("/engines")
    for {
      enginePath <- enginesPath
      actorPath <- zkClient.listChildPath(s"/engines/${enginePath}")
      remoteAddr = s"akka.tcp://RotorSystem@${enginePath}/user/${actorPath}"
      fullPath = s"/engines/${enginePath}/${actorPath}"
      state = zkClient.get(s"/engines/${enginePath}/${actorPath}") match {
        case Left(s) =>
          logger.error(s"Get actor: ${enginePath}/${actorPath} state failed, failed reason:" + s)
          "INVALID"
        case Right(s) => s
      }
    } yield EngineActor(remoteAddr, state, fullPath)

  }


}
