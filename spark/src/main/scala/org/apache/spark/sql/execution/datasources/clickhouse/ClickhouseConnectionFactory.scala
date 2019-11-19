package org.apache.spark.sql.execution.datasources.clickhouse

import java.util.Properties

import ru.yandex.clickhouse.ClickHouseDataSource
import ru.yandex.clickhouse.settings.ClickHouseProperties

object ClickhouseConnectionFactory extends Serializable{

  private var user: String = _
  private var password: String = _
  private val dataSources = scala.collection.mutable.Map[(String, Int), ClickHouseDataSource]()

  def setUserName(userName: String) = {
    user = userName
  }

  def setPassword(pass: String) = {
    password = pass
  }

  def get(host: String, port: Int = 8123, user: String = "", password: String = ""): ClickHouseDataSource ={
    dataSources.get((host, port)) match {
      case Some(ds) =>
        ds
      case None =>
        val ds = createDatasource(host, port, user, password)
        dataSources += ((host, port) -> ds)
        ds
    }
  }

  private def createDatasource(host: String, port:Int, user: String, password: String, dbO: Option[String] = None) = {
    val props = new Properties()
    dbO map {db => props.setProperty("database", db)}

    props.setProperty("user", user)
    props.setProperty("password", password)

    val clickHouseProps = new ClickHouseProperties(props)
    new ClickHouseDataSource(s"jdbc:clickhouse://$host:$port", clickHouseProps)
  }
}
