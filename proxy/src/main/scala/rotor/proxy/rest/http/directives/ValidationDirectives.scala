package rotor.proxy.rest.http.directives

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Directive, ValidationRejection}
import com.wix.accord.{Descriptions, Validator}

import scala.language.implicitConversions

trait ValidationDirectives {
  def validate(magnet: ValidationMagnet) = magnet()
}

object ValidationDirectives extends ValidationDirectives

/**
  * @see <a href="http://spray.io/blog/2012-12-13-the-magnet-pattern">Magnet Pattern</a>
  */
sealed trait ValidationMagnet {
  def apply(): Directive[Unit]
}

object ValidationMagnet {
  implicit def fromObj[T](obj: T)(implicit validator: Validator[T]) =
    new ValidationMagnet {
      def apply() = {

        val result = com.wix.accord.validate(obj)

        result match {
          case com.wix.accord.Success => pass
          case com.wix.accord.Failure(violations) => {
            val msg = violations map { v => Descriptions.render(v.path) } mkString ", "
            val verboseMsg = violations map { v => v.toString } mkString "; "
            reject(ValidationRejection(s"That was invalid:  ${msg}. ${verboseMsg}"))
          }
        }
      }
    }
}
