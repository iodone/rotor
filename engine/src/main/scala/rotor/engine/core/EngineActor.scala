package rotor.engine.core

/**
  * Created by iodone on {19-3-12}.
  */
import java.lang.reflect.Modifier

import akka.actor.{Actor, Props}
import org.apache.spark.scheduler.SparkListener
import org.apache.spark.sql.SparkSession
import rotor.common.utils.Zookeeper
import rotor.common.Logging
import rotor.common.domain.Entiy._
import rotor.engine.rql.RQLExecution
import rotor.engine.Launcher.createEngine

import scala.concurrent.Future


class EngineActor(rotorSession: RotorSession) extends Actor with Logging {

  import context.dispatcher

  var engine: SparkSession = _
  var actorPath: String = _

  def registerUDF(clazz: String) = {
    clazz.split(",").foreach {
      Class.forName(_).getMethods.foreach { f =>
        try {
          if (Modifier.isStatic(f.getModifiers)) {
            f.invoke(null, engine.udf)
          }
        } catch {
          case e: Exception =>
            e.printStackTrace()
        }
      }
    }
  }

  def registerEngineActor(): Unit = {
    val zkServers = rotorSession.appConf.getString("zookeeper.servers").split(",").map(_.trim)
    val zkPrefixPath = rotorSession.appConf.getString("zookeeper.prefix")
    val actorAddr = s"""${rotorSession.appConf.getString("akka.remote.netty.tcp.hostname")}:${rotorSession.appConf.getString("akka.remote.netty.tcp.port")}"""

    val zkClient = Zookeeper.getOrCreate(zkServers, zkPrefixPath)

    zkClient.write(s"/engines/${actorAddr}/${context.self.path.name}", "IDLE") match {
      case Left(errMsg) => {
        logger.fatal(s"Register engine actor: ${context.self.path.name} to zookeeper failed, reason for ${errMsg}")
        context.system.terminate()
      }
      case _ => actorPath = s"/engines/${actorAddr}/${context.self.path.name}"
    }
  }

  def addEngineListener(listener: SparkListener):Unit = engine.sparkContext.addSparkListener(listener)


  override def preStart(): Unit = {

    // create new engine
    engine = createEngine(rotorSession).newSession()

    // register internal UDF
    registerUDF("rotor.engine.udf.Functions")
    // register outer UDF
    val udfPath = rotorSession.engineConf.getOrElse("rotor.udfClassPath", "")
    if (udfPath != "") {
      registerUDF(udfPath)
    }

    // add engine job status listener
    addEngineListener(new JobExecution.JobStateListener)

    // register engine actor to zookeeper
    registerEngineActor()

    logger.warn(s"Engine actor: ${actorPath} started ...")

  }

  override def postStop(): Unit = {
    super.postStop()
    engine.stop

    val actorAddr = s"""${rotorSession.appConf.getString("akka.remote.netty.tcp.hostname")}:${rotorSession.appConf.getString("akka.remote.netty.tcp.port")}"""
    val zkClient = Zookeeper.getOrCreate
    zkClient.write(s"/engines/${actorAddr}/${context.self.path.name}", "DEAD")
  }

  override def receive: Receive = {

    case SubmitJob(name, rql) =>

      val resultPrefixPath = rotorSession.appConf.getString("engine.result-prefix")
      val jobId = JobExecution.prepare(engine.sparkContext.applicationId, name, actorPath, resultPrefixPath)

      sender ! JobExecution.allJobInfos.get(jobId)

      // 这里用Future不知道合适不？暂时可用, 不用Future会导致整个actor阻塞
      Future {
        JobExecution.execute(engine, rql, name, jobId, () => {
          val execContext = ExecutionContext(engine, jobId)
          RQLExecution.execute(rql, execContext)

          // drop temp view
          execContext.tmpTables.foreach { t =>
            engine.catalog.dropTempView(t)
          }
        })
      }

    case CancelJob(jobId) =>
      val jobInfo = JobExecution.kill(engine, jobId)
      sender ! jobInfo

    case StopEngineActor() =>
      context.system.terminate()

  }
}

object EngineActor {
  def props(rotorSession: RotorSession) = Props(new EngineActor(rotorSession))
}
