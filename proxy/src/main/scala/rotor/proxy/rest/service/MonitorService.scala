package rotor.proxy.rest.service

import com.typesafe.config.ConfigFactory
import rotor.common.domain.Entiy.{CancelJob, JobInfo, JobState, SubmitJob}
import rotor.common.utils.Zookeeper
import rotor.proxy.rest.common.EngineActorPool
import rotor.proxy.rest.common.EngineActorPool.logger
import rotor.proxy.rest.exception._
import rotor.proxy.rest.entiy.ResponseEntity._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

/**
  * Created by iodone on {19-4-11}.
  */
class MonitorService(implicit executionContext: ExecutionContext) extends Service {

  def describe(addr: String): Future[EngineStats] = Future {
    val zkClient = Zookeeper.getOrCreate()
    val actorsState = for {
      actorPath <- zkClient.listChildPath(s"/engines/${addr}")
      state = zkClient.get(s"/engines/${addr}/${actorPath}") match {
        case Left(s) =>
          logger.error(s"Get actor: ${addr}/${actorPath} state failed, failed reason:" + s)
          "INVALID"
        case Right(s) => s
      }
    } yield state

    actorsState.foldLeft(EngineStats(0,0,0,0)) { (es, s) =>
      val acc = s match {
        case JobState.RUNNING | JobState.SUBMITTED => ( es.all + 1, es.busy + 1, es.idle, es.dead)
        case JobState.FAILED | JobState.SUCCEEDED | JobState.KILLED | "IDLE" => (es.all + 1, es.busy, es.idle + 1, es.dead)
        case "INVALID" => (es.all + 1, es.busy, es.idle, es.dead + 1)
      }
      EngineStats.tupled(acc)
    }
  }
}