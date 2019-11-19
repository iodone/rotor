package rotor.engine.core

import java.util.concurrent.ConcurrentHashMap

import org.apache.spark.sql.SparkSession

import scala.collection.mutable.ArrayBuffer

/**
  * Created by iodone on {19-3-27}.
  */
case class ExecutionContext(engine: SparkSession, jobId: String, pathPrefix: String = "/tmp/rql/") {
  val dbInfo = new ConcurrentHashMap[String, Map[String, String]]()
  val tmpTables = ArrayBuffer[String]()
}
