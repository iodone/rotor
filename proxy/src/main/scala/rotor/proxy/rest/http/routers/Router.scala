package rotor.proxy.rest.http.routers

/**
  * Created by iodone on 2018/5/4.
  */


import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server._
import akka.http.scaladsl.model._
import com.typesafe.scalalogging.LazyLogging
import Directives._
import akka.http.scaladsl.model.headers.RawHeader
import io.circe.syntax._
import io.circe.generic.auto._

import scala.concurrent.ExecutionContext
import java.util.UUID

import akka.stream.ActorMaterializer
import rotor.proxy.rest.entiy.ResponseEntity.{Meta, Response}
import rotor.proxy.rest.service.{RQLService, MonitorService}
import api._


object RejectionHandlers {
  implicit val rejectionHandler = RejectionHandler.default
    .mapRejectionResponse {
      case res @ HttpResponse(_, _, ent: HttpEntity.Strict, _) =>
        // since all Akka default rejection responses are Strict this will handle all rejections
        val message = ent.data.utf8String.replaceAll("\"", """\"""")

        // we copy the response in order to keep all headers and status code, wrapping the message as hand rolled JSON
        // you could the entity using your favourite marshalling library (e.g. spray json or anything else)
        val resContent = Response(Meta(7000100, message), Nil).asJson.noSpaces
        res.copy(entity = HttpEntity(ContentTypes.`application/json`, resContent))
      // pass through all other types of responses
      case x => {
        x
      }
    }
}

object ExceptionHandlers extends LazyLogging {
  implicit val exceptionHandler = ExceptionHandler {
    case e: Exception =>
      logger.error("Internal error.", e)
      val resContent = Response(Meta(7000200, "Internal error"), Nil).asJson.noSpaces
      complete(500 -> HttpEntity(ContentTypes.`application/json`, resContent))
  }

}

class Router(implicit executionContext: ExecutionContext, implicit val actorMeter: ActorMaterializer) extends LazyLogging {


  // add route and bind service
  private val rqlRoute = new RQLRoute(new RQLService)
  private val monitorRoute = new MonitorRoute(new MonitorService)


  val routes: Route =
    logReqInfo {
      pathPrefix("api") {
        pathPrefix("v1") {
          rqlRoute.routes ~
          monitorRoute.routes
        }
    } ~
      pathPrefix("healthcheck") {
        get {
            complete(HttpEntity(ContentTypes.`application/json`, """{"healthStatus": "OK"}"""))
          }
        }
      }

  def logReqInfo = extractRequestContext.flatMap { ctx =>
    val start = System.currentTimeMillis()
    // handling rejections here so that we get proper status codes
    mapResponse { resp =>
      val d = System.currentTimeMillis() - start
      val reqId = ctx.request.headers.find(_.name == "X-Request-Id").map(_.value).getOrElse(UUID.randomUUID.toString)
      logger.info(s"[request-and-respone-info] [${reqId}] [${resp.status.intValue()}] ${ctx.request.method.name} ${ctx.request.uri} took: ${d}ms")
      resp.withHeaders(RawHeader("X-Request-Id", reqId))

    } & handleRejections(RejectionHandlers.rejectionHandler) & handleExceptions(ExceptionHandlers.exceptionHandler)
  }

}