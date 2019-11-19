package rotor.common
import akka.io.Tcp.Message
import org.apache.log4j.Logger

/**
  * Created by iodone on {19-3-4}.
  */
trait Logging {
  val logger = Logger.getLogger(this.getClass)

  def trace(message: => Any) = {
    if (logger.isTraceEnabled) {
      logger.trace(message)
    }
  }
  def debug(message: => Any) = {
    if (logger.isDebugEnabled) {
      logger.debug(message)
    }
  }

  def info(message: => Any) = {
    if (logger.isInfoEnabled) {
      logger.info(message)
    }
  }

  def warn(message: => Any) = {
    logger.warn(message)
  }

  def warn(message: => Any, t: Throwable) = {
    logger.warn(message, t)
  }

  def error(message: => Any, t: Throwable) = {
    logger.error(message, t)
  }
}
