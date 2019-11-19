package rotor.proxy.rest.service

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import io.circe.parser.decode
import io.circe.generic.auto._
import rotor.common.domain.Entiy.{CancelJob, JobInfo, JobState, SubmitJob}
import rotor.common.utils.Zookeeper
import rotor.proxy.rest.common.EngineActorPool
import rotor.proxy.rest.exception._

/**
  * Created by iodone on {19-4-11}.
  */
class RQLService(implicit executionContext: ExecutionContext) extends Service {

  val system = ActorSystem("Engine-Client", ConfigFactory.parseString(
    """
      | akka {
      |   actor {
      |     provider = "akka.remote.RemoteActorRefProvider"
      |   }
      |   remote {
      |     netty.tcp = {
      |       port = 9552
      |     }
      |   }
      | }
      """.stripMargin))

  implicit val timeout = Timeout(5 seconds)

  def submit(rql: String, name: String): Future[JobInfo] = Future {

    val idleActor = EngineActorPool.selectIdleActor match {
      case None => throw ResourceInsufficientException("执行引擎资源不足")
      case Some(x) => x
    }

    val remoteActor = system.actorSelection(idleActor.remoteAddr)
    val resF = (remoteActor ? SubmitJob(name, rql)).mapTo[JobInfo]
    val jobInfo = Await.result(resF, timeout.duration)
    EngineActorPool.giveBackActor(idleActor)
    jobInfo
  }

  def fetchJobInfo(jobId: String): Future[Option[JobInfo]] = Future {
    val zkClinet = Zookeeper.getOrCreate()
    zkClinet.get(s"/jobs/${jobId}").fold(_ => None, x => {
      decode[JobInfo](x).fold(_ => None, Some(_))
    })
  }

  def cancelJob(jobId: String): Future[Option[JobInfo]] = {
    val jobF = fetchJobInfo(jobId)
    jobF.map {
      case None => None
      case Some(jobInfo) =>
        if (jobInfo.jobState == JobState.SUBMITTED || jobInfo.jobState == JobState.RUNNING) {
          val subPaths = jobInfo.jobActorPath.split("/")
          val remoteAddr = s"akka.tcp://RotorSystem@${subPaths(2)}/user/${subPaths(3)}"
          val remoteActor = system.actorSelection(remoteAddr)
          val resF = (remoteActor ? CancelJob(jobId)).mapTo[JobInfo]
          Some(Await.result(resF, timeout.duration))
        } else {
          Some(jobInfo)
        }
    }

  }


}