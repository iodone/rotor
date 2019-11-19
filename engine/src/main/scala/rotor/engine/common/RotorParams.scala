package rotor.engine.common

/**
  * Created by iodone on {19-3-12}.
  */

class RotorParams(val store: Map[String, String]) {

  def getOrElse(k: String, defaultValue: String): String = {
    store.getOrElse(k, defaultValue)
  }

  def set(k: String, v: String) = {
    store + (k -> v)
  }
  def contains(k: String): Boolean = {
    store.contains(k)
  }
}

object RotorParams {
  def apply(params: List[String]) = {
    val s = params.grouped(2).foldLeft(Map[String,String]()) {
      (s, xs) => xs match {
        case List(a, b) =>  if (a.trim.startsWith("-rotor.") || a.trim.startsWith("-spark."))
          s + (a.trim.tail -> b.trim)
          else s
        case _ => s
      }
    }
    new RotorParams(s)
  }
}
