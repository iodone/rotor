package org.apache.spark.sql.execution.datasources.clickhouse

object Utils {
  def using[A, B <: {def close(): Unit}] (closeable: B) (f: B => A): A =
    try {
      f(closeable)
    }
    finally {
      closeable.close()
    }
}
