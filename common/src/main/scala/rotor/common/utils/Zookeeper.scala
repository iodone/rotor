package rotor.common.utils

import java.util.{Observable, Observer}

import scala.collection.JavaConversions._
import org.apache.curator.framework.recipes.cache._
import org.apache.curator.framework.{CuratorFramework, CuratorFrameworkFactory}
import org.apache.curator.retry.ExponentialBackoffRetry
import org.apache.zookeeper.KeeperException.NoNodeException
import rotor.common.Config

import scala.util.{Success, Try}

object Zookeeper {
  var zk: Zookeeper = null

  def getOrCreate(servers: Array[String] = Array[String](), pathPrefix: String = "") = synchronized {
    if (zk == null) {
      zk = new Zookeeper(servers.mkString(","), pathPrefix)
    }
    zk
  }

  def getOrCreate() = synchronized {
     if (zk == null) {
        val zkServers = Config.conf.zookeeper.servers.split("\\,").map(_.trim)
        val zkPrefixPath = Config.conf.zookeeper.prefix
        zk = new Zookeeper(zkServers.mkString(","), zkPrefixPath)
    }
    zk
  }
}

class Zookeeper extends Observable {

  private var client: CuratorFramework = _
  private var pathPrefix: String = _

  def this(connectString: String, pathPrefix: String) = {
    this()
    this.pathPrefix = pathPrefix
    client = CuratorFrameworkFactory.builder.connectString(connectString)
      .sessionTimeoutMs(3000)
      .retryPolicy(new ExponentialBackoffRetry(1000, 3))
      .build
    client.start
  }

  def getCuratorFramework = {
    this.client
  }

  private def initPath(path: String) = {
    path.tail.split("\\/").map("/" + _)
      .foldLeft("") { (m,n) =>
        create(m+n)
        m + n
      }
  }

  protected override def notifyObservers(arg: Any): Unit = {
    val notifier = arg.asInstanceOf[Notifier]
    notifier.observer.update(this, notifier)
  }

  def exists(path: String): Boolean = {
    client.checkExists.forPath(path) != null
  }

  def listChildPath(path: String): List[String] = {
    try {
      client.getChildren.forPath(this.pathPrefix + path).toList
    } catch {
      case _:NoNodeException => List()
    }
  }

  private def create(path: String, data: String = ""): Either[String, String] = {
    if(!exists(path)) {
      client.create.forPath(path, data.getBytes)
      Right.empty
    } else Left("This path has exist.")
  }

  def get(path: String): Either[String, String] = {
    val absolutePath = this.pathPrefix + path
    Try(client.getData.forPath(absolutePath)).transform(x => Success(Right(new String(x))), f => Success(Left(f.getMessage))).get
  }

  def write(path: String, data: String): Either[String, String] = {

    val absolutePath = this.pathPrefix + path
    if(!exists(absolutePath)) {
      initPath(absolutePath)
    }

    Try(client.setData.forPath(absolutePath, data.getBytes)).transform(_ => Success(Right("")), f => Success(Left(f.getMessage))).get
  }

  def close = {
    client.close()
  }

  def delete(path: String): Either[String, String] = {
    val absolutePath = this.pathPrefix + path
    if(exists(absolutePath)) {
      client.delete.forPath(absolutePath)
      Right.empty
    } else Left.empty
  }

  def deleteAll(path: String): Either[String, String] = {
    val absolutePath = this.pathPrefix + path
    if(exists(absolutePath)) {
      client.delete.deletingChildrenIfNeeded().forPath(absolutePath);
      Right.empty
    } else Left.empty
  }

  def watch(observer: Observer, path: String): TreeCache = {
    val treeCache = new TreeCache(client, path)
    treeCache.getListenable.addListener(new TreeCacheListener {
      override def childEvent(client: CuratorFramework, event: TreeCacheEvent) = {
        val notifier = Notifier(observer, event)
        notifyObservers(notifier)
      }
    })
    treeCache.start
  }



//  implicit class ClientExtension(client: CuratorFramework) {
//    def getString(path: String): String = {
//      val data = client.getData.forPath(path)
//      val kryo = new Kryo
//      val input = new Input(data)
//      val content = kryo.readObject(input, classOf[String])
//      input.close
//      content
//    }
//  }

  implicit class EmptyRight(right: util.Right.type) {
    def empty = Right("")
  }

  implicit class EmptyLeft(right: util.Left.type) {
    def empty = Left("")
  }
}
case class Notifier(observer: Observer, event: TreeCacheEvent) {

  def change: Change = {
    val data = event.getData
    Change(data.getPath, event.getType, new String(data.getData))
  }
}

case class Change(path: String, eventType: TreeCacheEvent.Type, content: String)

