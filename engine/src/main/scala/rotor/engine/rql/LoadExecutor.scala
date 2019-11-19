package rotor.engine.rql

/**
  * Created by iodone on {19-3-27}.
  */

import java.security.InvalidParameterException

import rotor.engine.rql.Ast.Action
import rotor.engine.core.ExecutionContext

case class LoadExecutor(executionContext: ExecutionContext) extends Executor[Ast.Action.LoadAction] {

  override def execute(action: Action.LoadAction): Unit = {

    val format = action.source.format.getText
    val path = cleanStr(action.source.path.getText)
    val tableName = action.table.getText
    var options = action.where match {
      case None => Map(Seq[(String, String)]():_*)
      case Some(w) =>
        val optionsClean = w.getText.map {
          case (fst, snd) => (cleanStr(fst), cleanStr(snd))
        }
        Map(optionsClean:_*)
    }

    val reader = executionContext.engine.read
    if (options.contains("schema")) {
      reader.option("schema", options("schema"))
      options = options - "schema"
    }
    reader.options(options)

    val table = format match {
      case "jdbc" =>
        val pathSlices = path.split("\\.")
        if (pathSlices.length != 2) {
          throw new InvalidParameterException("In rql load table must join database and table with . ")
        }
        reader.option("dbtable", pathSlices(1))
        val options = executionContext.dbInfo.get(pathSlices(0))
        if (options == null) {
          throw new InvalidParameterException(s"In rql load database: ${pathSlices(0)} is invalid.")
        }
        reader.options(options)
        reader.format("jdbc").load
      case "es" | "org.elasticsearch.spark.sql" =>
        val pathSlices = path.split("\\.")
        if (pathSlices.length != 2) {
          throw new InvalidParameterException("In rql load table must join database and table with . ")
        }
        if (options == null) {
          throw new InvalidParameterException(s"In rql load database: ${pathSlices(0)} is invalid.")
        }
        reader.options(options)
        reader.format("jdbc").load(pathSlices(1))
      case "hive" =>
        reader.table(cleanStr(path))
      case "carbon" =>
        reader.table(cleanStr(path))
      case "hbase" | "org.apache.spark.sql.execution.datasources.hbase" =>
        reader.option("inputTableName", path).format("org.apache.spark.sql.execution.datasources.hbase").load
      case "ck" =>
        val pathSlices = path.split("\\.")
        if (pathSlices.length != 2) {
          throw new InvalidParameterException("In rql load table must join database and table with . ")
        }
        reader.option("dbtable", pathSlices(1))
        val options = executionContext.dbInfo.get(pathSlices(0))
        if (options == null) {
          throw new InvalidParameterException(s"In rql load database: ${pathSlices(0)} is invalid.")
        }
        reader.options(options)
        reader.option("driver", "ru.yandex.clickhouse.ClickHouseDriver")
        reader.format("jdbc").load
      case _ =>
        reader.format(format).load(path)
    }

    table.createOrReplaceTempView(tableName)
    executionContext.tmpTables += tableName
  }

}
