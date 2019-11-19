package rotor.common.domain

/**
  * Created by iodone on {19-4-2}.
  */
object Entiy {

  // Message defines
  case class TestRequest(t: String)
  case class SubmitJob(name: String, rql: String)
  case class CancelJob(jobId: String)
  case class StopEngineActor()
  case class GetJobStatus(jobId: String)

  case object JobState {
    val SUBMITTED = "SUBMITTED"
    val RUNNING = "RUNNING"
    val SUCCEEDED = "SUCCEEDED"
    val FAILED = "FAILED"
    val KILLED = "KILLED"
  }

  case class JobInfo(applicationId: String = "",
                     jobId: String = "",
                     jobName: String = "",
                     jobState: String = "",
                     jobActorPath: String = "",
                     submitTime: Long = -1,
                     startTime: Long = -1,
                     endTime: Long = -1,
                     failedMsg: String = "",
                     jobResultPath: String = "")
}
