package rotor.engine.rql

import org.apache.spark.sql.{SaveMode, SparkSession}
import rotor.engine.core.{ExecutionContext, JobExecution}

/**
  * Created by iodone on {19-3-27}.
  */
object RQLExecution {

  def execute(rql: String, execContext: ExecutionContext) = {
    RQLParser.parseRql(rql).foreach {
      case x: Ast.Action.ConnectAction => ConnectExecutor(execContext).execute(x)
      case x: Ast.Action.LoadAction => LoadExecutor(execContext).execute(x)
      case x: Ast.Action.SelectAction => SelectExecutor(execContext).execute(x)
    }

    val df =  execContext.engine.table(execContext.tmpTables.last).limit(10000)
    val savaPath = JobExecution.allJobInfos.get(execContext.jobId).jobResultPath
    df.repartition(1).write.format("json").mode(SaveMode.Overwrite).save(savaPath)

  }
}

trait Executor[T <: Ast.Action] {
  def execute(action: T): Unit

  def cleanStr(str: String) = {
    if (str.startsWith("`") || str.startsWith("\"")) {
      str.slice(1, str.length-1)
    } else str

  }
}

