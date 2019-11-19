package rotor.proxy.rest

/**
  * Created by iodone on {19-2-14}.
  */

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import rotor.common.domain.Entiy._

import scala.concurrent.Await
import scala.concurrent.duration._


object ProxyApp extends App {
  println("hello world ...")
  val system = ActorSystem("EngineClient", ConfigFactory.parseString(
    """
      |akka {
      |       actor {
      |          provider = "akka.remote.RemoteActorRefProvider"
      |        }
      |}
    """.stripMargin))
  val remoteActor1 = system.actorSelection("akka.tcp://RotorSystem@192.168.65.165:30000/user/1")


  def testActor = {

    implicit val timeout = Timeout(11 seconds)
    val startTime = System.currentTimeMillis
    val resF1 = (remoteActor1 ? SubmitJob("11", "select 1 as a as test_0;" )).mapTo[JobInfo]
    Thread.sleep(2000)
    val resF2 = (remoteActor1 ? CancelJob("1111")).mapTo[JobInfo]

    val res2 = Await.result(resF2, timeout.duration)
    println(s"After: ${System.currentTimeMillis() - startTime} reicive result2:" + res2)

    val res1 = Await.result(resF1, timeout.duration)
    println(s"After: ${System.currentTimeMillis() - startTime} reicive result1:" + res1)

  }


  def testHive = {
    val rql0 =
      """
        |load hive.`hive_cc_8102` as hive_test0;
        |select * from hive_test0 as t0;
        |select * from t0 where hash = "0008BBB7A2F056BEED48BE89BD4CDD75" as t1;
      """.stripMargin
    val rql1 =
      """
        |connect ck where `url`="jdbc:clickhouse://192.168.200.151:8123" and user="default" and password="xxxx" as db1;
        |load  ck.`eds_app_detection_local` as t0;
        |select * from t0 where log_id = "b37d63d3-4829-4609-b9eb-866e3b75b91a-b94097bde9" as t1;
      """.stripMargin

    implicit val timeout = Timeout(11 seconds)
    val startTime = System.currentTimeMillis
    val resF1 = (remoteActor1 ? SubmitJob("hive_test", rql0)).mapTo[JobInfo]
    val resF2 = (remoteActor1 ? SubmitJob("hive_test", rql1)).mapTo[JobInfo]
    val res1 = Await.result(resF1, timeout.duration)
    println(s"After: ${System.currentTimeMillis() - startTime} reicive result:" + res1)
    val res2 = Await.result(resF2, timeout.duration)
    println(s"After: ${System.currentTimeMillis() - startTime} reicive result:" + res2)
  }
  // testHive

  def testClickHouse = {
    val rql0 =
      """
        |connect ck where `url`="jdbc:clickhouse://192.168.200.151:8123/default" and user="default" and password="xxxx" as db1;
        |load  ck.`db2.eds_app_detection_local` as t0;
        |select * from t0 where log_id = "b37d63d3-4829-4609-b9eb-866e3b75b91a-b94097bde9" as t1;
      """.stripMargin

    implicit val timeout = Timeout(11 seconds)
    val startTime = System.currentTimeMillis
    val resF1 = (remoteActor1 ? SubmitJob("hive_test", rql0)).mapTo[JobInfo]
    val res1 = Await.result(resF1, timeout.duration)
    println(s"After: ${System.currentTimeMillis() - startTime} reicive result:" + res1)
  }
  testClickHouse

  def testHbase = {
    val rql0 =
      """
        |load hbase.`hbase-user-label-test` options `zk`="192.168.12.227:2181"
        |and `family`="info" as rql_example;
        |
        |select * from xql_example where rowkey = "000001f39ac26739868479023166330" as show_data;
        |
      """.stripMargin
    implicit val timeout = Timeout(11 seconds)
    val startTime = System.currentTimeMillis
    val resF1 = (remoteActor1 ? SubmitJob("hbase_test", rql0)).mapTo[JobInfo]
    val res1 = Await.result(resF1, timeout.duration)
    println(s"After: ${System.currentTimeMillis() - startTime} reicive result:" + res1)
  }
//  testHbase

  def testJDBC = {
     val rql0 =
      """
        |connect jdbc where `url`="jdbc:mysql://192.168.8.16:4000/database?characterEncoding=utf8" and user="root" and password="xxx" as db1;
        |load jdbc.`db1.tidb_attack_info` as t0;
        |select * from t0 as t1;
      """.stripMargin

    implicit val timeout = Timeout(11 seconds)
    val startTime = System.currentTimeMillis
    val resF1 = (remoteActor1 ? SubmitJob("jdbc_test", rql0)).mapTo[JobInfo]
    val res1 = Await.result(resF1, timeout.duration)
    println(s"After: ${System.currentTimeMillis() - startTime} reicive result:" + res1)
  }
//  testJDBC

  def testHDFS = {
     val rql0 =
      """
        |load json.`/tmp/rql/c4eb361c-c0bc-4c43-9950-ae05d9e906d2` as t0;
        |select * from t0 as t1;
      """.stripMargin

      implicit val timeout = Timeout(11 seconds)
      val startTime = System.currentTimeMillis
      val resF1 = (remoteActor1 ? SubmitJob("json_test", rql0)).mapTo[JobInfo]
      val res1 = Await.result(resF1, timeout.duration)
      println(s"After: ${System.currentTimeMillis() - startTime} reicive result:" + res1)

  }
//  testHDFS

}


