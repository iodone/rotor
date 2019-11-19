package rotor.engine

/**
  * Created by iodone on {19-4-3}.
  */
object TestApp0 extends App {
  Launcher.main(Array(
    "-rotor.name", "test0",
    "-rotor.engine.worker.parallelism", "4",
    "-rotor.carbondata.store", "hdfs://nameservice1/data/CarbonStore/data/store",
    "-spark.master", "local[1]"
  ))
}
