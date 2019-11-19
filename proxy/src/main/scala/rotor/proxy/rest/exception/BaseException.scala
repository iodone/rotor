package rotor.proxy.rest.exception

/**
  * Created by iodone on {19-4-12}.
  */

import akka.http.scaladsl.model.StatusCode
import akka.http.scaladsl.model.StatusCodes._

abstract class BaseException(val errorCode: Int, val message: String, val status: StatusCode) extends Exception(message)

case class ValidateError(override val message: String)
  extends BaseException(7000401, message, BadRequest)

case class ResourceInsufficientException(override val message: String = "资源不足")
  extends BaseException(7000402, message, InternalServerError)

