package rotor.engine.rql

/**
  * Created by iodone on {19-3-27}.
  */

import rotor.engine.rql.Ast.Action
import rotor.engine.core.ExecutionContext

case class SelectExecutor(executionContext: ExecutionContext) extends Executor[Ast.Action.SelectAction] {

  override def execute(action: Action.SelectAction): Unit = {
    val rawSelect = action.body
    val parttern = "as[\\s\\t\\r\\n]+([a-zA-z0-9|_]+|`[^`]*`)$"
    val sql = rawSelect.replaceAll(parttern, "").replaceAll("[\\s\\t\\r\\n]+$", "")
    val tableName = parttern.r.findFirstIn(rawSelect) match {
      case None => "tmpTableName"
      case Some(t) => t.replaceAll("^as[\\s\\t\\r\\n]+", "")
    }
    executionContext.engine.sql(sql).createOrReplaceTempView(tableName)
    executionContext.tmpTables += tableName
  }

}
