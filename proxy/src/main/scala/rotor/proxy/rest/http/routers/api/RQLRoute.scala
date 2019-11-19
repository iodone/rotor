package rotor.proxy.rest.http.routers.api

/**
  * Created by iodone on {19-4-11}.
  */

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.model._
import com.typesafe.scalalogging.LazyLogging
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport

import scala.concurrent.ExecutionContext
import io.circe.Json
import io.circe.optics.JsonPath._
import io.circe.syntax._
import io.circe.generic.auto._
import rotor.proxy.rest.service.RQLService
import rotor.proxy.rest.entiy.RequestEntity._
import rotor.proxy.rest.entiy.ResponseEntity._
import rotor.proxy.rest.http.directives.ValidationDirectives
import rotor.proxy.rest.exception._

import rotor.common.domain.Entiy.JobInfo

class RQLRoute(rqlService: RQLService)(implicit executionContext: ExecutionContext) extends FailFastCirceSupport with LazyLogging {

  import StatusCodes._
  import rqlService._

  object RQLValidators  {
    import com.wix.accord.dsl._
    import com.wix.accord._

    implicit val rqlContentValidator = validator[RqlContent] { p =>
      p.name is notEmpty
      p.rql is notEmpty
    }

    implicit val jobValidator = validator[Job] { p =>
      p.id is notEmpty
    }
  }

  val routes: Route = pathPrefix("rql") {
    import RQLValidators._
    import ValidationDirectives._

    pathPrefix("job") {
      path("submit") {
        post {
          entity(as[RqlContent]) { rc =>
            validate(rc) {
              val respF = submit(rc.rql, rc.name) map { jobInfo =>
                OK -> Response(Meta(0, ""), jobInfo).asJson
              } recover {
                case ex: BaseException => ex.status -> Response(Meta(ex.errorCode, ex.message), Nil).asJson
              }
              complete(respF)
            }
          }
        }
      } ~
        path("discribe") {
          post {
            entity(as[Job]) { job =>
              validate(job) {
                val respF = fetchJobInfo(job.id) map {
                  case None => NotFound -> Response(Meta(7000401, "Job not exist."), Nil).asJson
                  case Some(jobInfo) => OK -> Response(Meta(0, ""), jobInfo).asJson
                } recover {
                  case ex: BaseException => ex.status -> Response(Meta(ex.errorCode, ex.message), Nil).asJson
                }
                complete(respF)
              }
            }
          }
        } ~
        path("cancel") {
          post {
            entity(as[Job]) { job =>
              validate(job) {
                val respF = cancelJob(job.id) map {
                  case None => NotFound -> Response(Meta(7000401, "Job not exist."), Nil).asJson
                  case Some(jobInfo) => OK -> Response(Meta(0, ""), jobInfo).asJson
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
