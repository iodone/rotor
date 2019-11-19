package rotor.engine.rql

/**
  * Created by iodone on {19-3-27}.
  */

import rotor.engine.core.ExecutionContext

case class ConnectExecutor(executionContext: ExecutionContext) extends Executor[Ast.Action.ConnectAction] {

  override def execute(action: Ast.Action.ConnectAction): Unit = {
    val db = action.db.getText
    executionContext.dbInfo.put(db, Map(("format", action.format.getText)))
    action.where match {
      case Some(w) =>
        val options = w.getText.map {
          case (fst, snd) => (cleanStr(fst), cleanStr(snd))
        }
        executionContext.dbInfo.put(db,Map(options: _*))
    }
  }

}
