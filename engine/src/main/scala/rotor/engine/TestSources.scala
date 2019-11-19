package rotor.engine

import org.apache.spark.sql.SparkSession

/**
  * Created by iodone on {19-4-10}.
  */
object TestSources {

  def main0(args: Array[String]): Unit = {
    Class.forName("ru.yandex.clickhouse.ClickHouseDriver")
    val ss = SparkSession
      .builder()
      .master("local[2]")
      .getOrCreate()
    val jdbcDF = ss.read
      .format("jdbc")
      .option("url", "jdbc:clickhouse://192.168.200.151:8123/default")
      .option("dbtable", "eds_app_detection_local")
      .option("user", "default")
      .option("password", "1Qaaz2Wsx")
      .load()
    import ss.implicits._
    val df2 = jdbcDF.filter($"log_id" === "b37d63d3-4829-4609-b9eb-866e3b75b91a-b94097bde9")
    df2.show()
  }

  def main(args: Array[String]): Unit = {


    try {
      throw new NullPointerException("hello")
    } catch {
      case e: Throwable =>
//        e.printStackTrace()
        val errMsg = e.getMessage match {
          case null =>
            e.getCause.toString
          case _ =>
            e.getMessage.split("\n")(0)
        }
        println("Throwable")
        println(errMsg)
      case e: RuntimeException =>
        e.printStackTrace()
        val errMsg = e.getMessage match {
          case null =>
            e.getCause.toString
          case _ =>
            e.getMessage.split("\n")(0)
        }

        println("RuntimeException")
        println(errMsg)
    } finally {
      println("fanally..")
    }

  }
}
