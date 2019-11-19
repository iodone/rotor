package rotor.proxy.rest

/**
  * Created by iodone on {19-4-11}.
  */

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.{ExecutionContext, Future}

import rotor.common.{HttpConfig, Config}
import rotor.proxy.rest.http.routers.Router

class RestServer(config: HttpConfig, actorSystem: ActorSystem) extends LazyLogging {

  implicit val system: ActorSystem = actorSystem
  implicit val executionContext: ExecutionContext = system.dispatcher
  implicit val materializer = ActorMaterializer()


  var bindingFuture: Future[ServerBinding] = _

  def start(): Unit = {
    val router = new Router
    bindingFuture = Http().bindAndHandle(router.routes, config.host, config.port)
    logger.info(s"Waiting for requests at http://${config.host}:${config.port}/...")
  }

  def stop(): Unit = {
    if (bindingFuture != null) {
      bindingFuture
        .flatMap(_.unbind())
        .onComplete(_ => system.terminate)
    }
  }
}

object RestServer {

  def apply(config: HttpConfig, actorSystem: ActorSystem): RestServer =
    new RestServer(config, actorSystem)
}
