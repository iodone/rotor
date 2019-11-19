package rotor.proxy.rest.entiy

 // Response entity
// standard request entity for json response
object ResponseEntity {
  final case class Meta(errCode: Int, errMsg: String)
  final case class Response[T](meta: Meta, data: T)

  case class EngineStats(all: Int, busy: Int, idle: Int, dead: Int)

}
// entities to be resolved from json request
object RequestEntity {
  final case class RqlContent(rql: String, name: String)
  final case class Job(id: String)

  final case class Engine(address: String)

}

object Gender extends Enumeration {
  val M = Value("M")
  val F = Value("F")
}

