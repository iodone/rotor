package rotor.engine.core

/**
  * Created by iodone on {19-3-21}.
  */
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

import org.apache.spark.scheduler.SparkListener
import org.apache.spark.scheduler.SparkListenerJobStart
import org.apache.spark.sql.SparkSession
import io.circe.syntax._
import io.circe.generic.auto._
import rotor.common.utils.Zookeeper
import rotor.common.Logging


object JobExecution extends Logging {

  import rotor.common.domain.Entiy._
  // save all job runtime status
  val allJobInfos = new ConcurrentHashMap[String, JobInfo]()

  // job state listener
  class JobStateListener extends SparkListener with Logging {
    override def onJobStart(jobStart: SparkListenerJobStart): Unit = {
      val jobId = jobStart.properties.getProperty("spark.jobGroup.id")
      if (jobId != null) {
        logger.warn(s"Job ${jobId} start at ${jobStart.time} with stage ${jobStart.stageIds}")
        updateJobInfo(JobInfo(jobId=jobId, jobState=JobState.RUNNING))
      }

    }
  }

  def prepare(appId: String, jobName: String, actorPath: String, resultPrefixPath: String): String = {
    val jobId = UUID.randomUUID().toString
    updateJobInfo(JobInfo(appId, jobId, jobName, JobState.SUBMITTED, actorPath, jobResultPath = s"${resultPrefixPath}/${jobId}"))
    jobId
  }

  def execute(engine: SparkSession, rql: String, jobName: String, jobId: String, f: () => Unit) = {
    // call f

    val jobDesc = s"[jobName: ${jobName} -- groupId: ${jobId}] --rql:${rql}"

    logger.warn(jobDesc)
    engine.sparkContext.clearJobGroup()
    engine.sparkContext.setJobGroup(jobId, jobDesc, true)

    try {
      f()
      updateJobInfo(JobInfo(jobId=jobId, jobState = JobState.SUCCEEDED))

    } catch {
      case e: Throwable =>
        e.printStackTrace()
        val errMsg = e.getMessage match {
          case null =>
            e.getCause.toString
          case _ =>
            e.getMessage.split("\n")(0)
        }
        updateJobInfo(JobInfo(jobId=jobId, jobState=JobState.FAILED, failedMsg=errMsg))
    }
    finally {
      cleanJob(jobId)
    }
  }

  def kill(engine: SparkSession, jobId: String): JobInfo =  {
    engine.sparkContext.cancelJobGroup(jobId)
    val jobInfo = updateJobInfo(JobInfo(jobId=jobId, jobState=JobState.KILLED))
    cleanJob(jobId)
  }

  def cleanJob(jobId: String) = {
    allJobInfos.get(jobId).jobState match {
      case JobState.SUCCEEDED | JobState.FAILED | JobState.KILLED =>
        allJobInfos.remove(jobId)
    }
  }

  def updateJobInfo(jobInfo: JobInfo) = {

    val curJobInfo = allJobInfos.get(jobInfo.jobId)

    if (curJobInfo == null) {
      if (jobInfo.jobState == JobState.SUBMITTED)  {
        val newJobInfo = jobInfo.copy(submitTime=System.currentTimeMillis)
        allJobInfos.put(newJobInfo.jobId, newJobInfo)
        syncToZk(s"/jobs/${newJobInfo.jobId}", newJobInfo.asJson.noSpaces)
        syncToZk(newJobInfo.jobActorPath, newJobInfo.jobState)
      } else {
        logger.warn(s"Job:[${jobInfo.jobId}] is not RQL job or not in this engine actor.")
      }
    } else {
     val newJobInfo = jobInfo.jobState match {
       case JobState.RUNNING =>
         curJobInfo.copy(startTime = System.currentTimeMillis, jobState = jobInfo.jobState)
       case JobState.FAILED =>
         curJobInfo.copy(endTime = System.currentTimeMillis, jobState = jobInfo.jobState, failedMsg = jobInfo.failedMsg)
       case JobState.SUCCEEDED =>
         curJobInfo.copy(endTime = System.currentTimeMillis, jobState = jobInfo.jobState)
       case JobState.KILLED =>
         curJobInfo.copy(endTime = System.currentTimeMillis, jobState = jobInfo.jobState)
     }
      allJobInfos.put(newJobInfo.jobId, newJobInfo)
      syncToZk(s"/jobs/${newJobInfo.jobId}", newJobInfo.asJson.noSpaces)
      syncToZk(newJobInfo.jobActorPath, newJobInfo.jobState)
    }
  }

//  def refreshTable(engine: SparkSession): Unit = {
//    engine.catalog.listTables.collect.foreach { t =>
//      engine.catalog.dropTempView(t.name)
//    }
//
//    /*
//    for {
//      db <- engine.catalog.listDatabases.rdd.map(_.name).collect
//      t <- engine.catalog.listTables(db).rdd.map(_.name).collect
//    } {
//      engine.catalog.refreshTable(db + "." + t)
//    }
//    */
//  }


  def syncToZk(path: String, value: String) = {
    val zkClient = Zookeeper.getOrCreate()
    zkClient.write(path, value) match {
      case Left(msg) => logger.error(s"Write zookeeper failedd, key: ${path}, value: ${value}, reason for ${msg}")
      case Right(_) => logger.warn(s"Write zookeeper sucessfully, key: ${path}, value: ${value}")
    }
  }


}
