package rotor.proxy.rest.http.routers.api

/**
  * Created by iodone on {19-4-11}.
  */

import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.typesafe.scalalogging.LazyLogging
import com.wix.accord.{BaseValidator, NullSafeValidator, Validator, ViolationBuilder}
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.generic.auto._
import io.circe.syntax._
import rotor.proxy.rest.entiy.RequestEntity._
import rotor.proxy.rest.entiy.ResponseEntity._
import rotor.proxy.rest.exception._
import rotor.proxy.rest.http.directives.ValidationDirectives
import rotor.proxy.rest.service.MonitorService

import scala.concurrent.ExecutionContext
import scala.util.matching.Regex

class MonitorRoute(monitorService: MonitorService)(implicit executionContext: ExecutionContext) extends FailFastCirceSupport with LazyLogging {

  import StatusCodes._
  import monitorService._

  object MonitorValidators  {
    import com.wix.accord.dsl._
    import com.wix.accord._

    implicit val EngineValidator = validator[Engine] { p =>
      p.address is notEmpty
    }
  }

  val routes: Route = pathPrefix("monitor") {
    import MonitorValidators._
    import ValidationDirectives._

    pathPrefix("engine") {
      path("describe") {
        post {
          entity(as[Engine]) { e =>
            validate(e) {
              val respF = describe(e.address) map {  s =>
                OK -> Response(Meta(0, ""), s).asJson
              } recover {
                case ex: BaseException => ex.status -> Response(Meta(ex.errorCode, ex.message), Nil).asJson
              }
              complete(respF)
            }
          }
        }
      }
    }
  }
}
