package rotor.engine.common

/**
  * Created by iodone on {19-3-12}.
  */
import rotor.engine.BaseTest

class RotorParamsSpec extends BaseTest {
  "RotorParams" when {
    "new RotorParams" should {

      val rp = RotorParams(Array("-rotor.hello ", "  world", "-spark.foo", "bar").toList)
      "return new RotorParams with store is map" in {
        assert(rp.store == Map("rotor.hello" -> "world", "spark.foo" -> "bar"))
      }

      "call getOrElse" in {
        assert(rp.getOrElse("rotor.hello", "") == "world")
        assert(rp.getOrElse("world", "") == "")

      }

    }
  }
}
