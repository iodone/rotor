package org.apache.spark.sql.execution.datasources.clickhouse.spark

import org.apache.spark.sql.execution.datasources.clickhouse.{ClickhouseClient, ClickhouseConnectionFactory}
import ru.yandex.clickhouse.ClickHouseDataSource
import org.apache.spark.sql.execution.datasources.clickhouse.Utils._
import org.apache.spark.sql.types._

object ClickhouseSparkExt{
  implicit def extraOperations(df: org.apache.spark.sql.DataFrame) = DataFrameExt(df)
}

case class DataFrameExt(df: org.apache.spark.sql.DataFrame) extends Serializable {

  def dropClickhouseDb(dbName: String, clusterNameO: Option[String] = None)
                      (implicit ds: ClickHouseDataSource){
    val client = ClickhouseClient(clusterNameO)(ds)
    clusterNameO match {
      case None => client.dropDb(dbName)
      case Some(x) => client.dropDbCluster(dbName)
    }
  }

  def createClickhouseDb(dbName: String, clusterNameO: Option[String] = None)
                        (implicit ds: ClickHouseDataSource){
    val client = ClickhouseClient(clusterNameO)(ds)
    clusterNameO match {
      case None => client.createDb(dbName)
      case Some(x) => client.createDbCluster(dbName)
    }
  }

  def createClickhouseTable(dbName: String, tableName: String, user:String, password: String, partitionColumnName: String, indexColumns: Seq[String], clusterNameO: Option[String] = None)
                           (implicit ds: ClickHouseDataSource){
    val client = ClickhouseClient(clusterNameO)(ds)
    val sqlStmt = createClickhouseTableDefinitionSQL(dbName, tableName, partitionColumnName, indexColumns)
    clusterNameO match {
      case None =>
        client.query(sqlStmt)
      case Some(clusterName) =>
        // create local table on every node
        client.queryCluster(sqlStmt, user, password)
        // create distrib table (view) on every node
        val sqlStmt2 = s"CREATE TABLE IF NOT EXISTS ${dbName}.${tableName}_all AS ${dbName}.${tableName} ENGINE = Distributed($clusterName, $dbName, $tableName, rand());"
        client.queryCluster(sqlStmt2, user, password)
    }
  }

  def saveToClickhouse(dbName: String, tableName: String, user: String, password: String, partitionFunc: (org.apache.spark.sql.Row) => java.sql.Date, partitionColumnName: String = "mock_date", clusterNameO: Option[String] = None, batchSize: Int = 100000)
                      (implicit ds: ClickHouseDataSource)={

    val defaultHost = ds.getHost
    val defaultPort = ds.getPort

    val (clusterTableName, clickHouseHosts) = clusterNameO match {
      case Some(clusterName) =>
        // get nodes from cluster
        val client = ClickhouseClient(clusterNameO)(ds)
        (s"${tableName}_all", client.getClusterNodes())
      case None =>
        (tableName, Seq(defaultHost))
    }

    val schema = df.schema

    // following code is going to be run on executors
    val insertResults = df.rdd.mapPartitions((partition: Iterator[org.apache.spark.sql.Row])=>{

      val rnd = scala.util.Random.nextInt(clickHouseHosts.length)
      val targetHost = clickHouseHosts(rnd)
      val targetHostDs = ClickhouseConnectionFactory.get(targetHost, defaultPort, user, password)

      // explicit closing
      using(targetHostDs.getConnection) { conn =>

        val insertStatementSql = generateInsertStatment(schema, dbName, clusterTableName, partitionColumnName)
        val statement = conn.prepareStatement(insertStatementSql)

        var totalInsert = 0
        var counter = 0

        while(partition.hasNext){

          counter += 1
          val row = partition.next()

          // create mock date
          var partitionVal:java.sql.Date = null
          if (row.schema.fieldNames.contains(partitionColumnName)) {
            partitionVal = row.getAs[java.sql.Date](s"$partitionColumnName")
          } else {
            partitionVal = partitionFunc(row)
          }
          statement.setDate(1, partitionVal)

          // map fields
          schema.foreach{ f =>
            val fieldName = f.name
            val fieldIdx = row.fieldIndex(fieldName)
            val fieldVal = row.get(fieldIdx)
            if(fieldVal != null)
              statement.setObject(fieldIdx + 2, fieldVal)
            else{
              val defVal = defaultNullValue(f.dataType, fieldVal)
              statement.setObject(fieldIdx + 2, defVal)
            }
          }
          statement.addBatch()

          if(counter >= batchSize){
            val r = statement.executeBatch()
            totalInsert += r.sum
            counter = 0
          }

        } // end: while

        if(counter > 0) {
          val r = statement.executeBatch()
          totalInsert += r.sum
          counter = 0
        }

        // return: Seq((host, insertCount))
        List((targetHost, totalInsert)).toIterator
      }

    }).collect()

    // aggr insert results by hosts
    insertResults.groupBy(_._1)
      .map(x => (x._1, x._2.map(_._2).sum))
  }

  private def generateInsertStatment(schema: org.apache.spark.sql.types.StructType, dbName: String, tableName: String, partitionColumnName: String) = {
    val columns = partitionColumnName :: schema.map(f => f.name).toList
    val vals = 1 to (columns.length) map (i => "?")
    s"INSERT INTO $dbName.$tableName (${columns.mkString(",")}) VALUES (${vals.mkString(",")})"
  }

  private def defaultNullValue(sparkType: org.apache.spark.sql.types.DataType, v: Any) = sparkType match {
    case DoubleType => 0
    case LongType => 0
    case FloatType => 0
    case IntegerType => 0
    case StringType => null
    case BooleanType => false
    case _ => null
  }

  private def createClickhouseTableDefinitionSQL(dbName: String, tableName: String, partitionColumnName: String, indexColumns: Seq[String])= {

    val header = s"""
          CREATE TABLE IF NOT EXISTS $dbName.$tableName(
          """
    var dateCols: Set[String] = Set()
    df.schema.map { f=>
      if(sparkType2ClickhouseType(f.dataType) == "Date")
        dateCols += f.name
    }

    val columns = if(dateCols.contains(partitionColumnName))
      df.schema.map { f =>
        Seq(f.name, sparkType2ClickhouseType(f.dataType)).mkString(" ")
      }.toList
    else
      s"$partitionColumnName Date" :: df.schema.map{ f =>
        Seq(f.name, sparkType2ClickhouseType(f.dataType)).mkString(" ")
      }.toList
    val columnsStr = columns.mkString(",\n")

    val footer = s"""
          )ENGINE = MergeTree($partitionColumnName, (${indexColumns.mkString(",")}), 8192);
          """

    Seq(header, columnsStr, footer).mkString("\n")
  }

  private def sparkType2ClickhouseType(sparkType: org.apache.spark.sql.types.DataType)= sparkType match {
    case LongType => "Int64"
    case DoubleType => "Float64"
    case FloatType => "Float32"
    case IntegerType => "Int32"
    case StringType => "String"
    case BooleanType => "UInt8"
    case DateType => "Date"
    case _ => "unknown"
  }

}
