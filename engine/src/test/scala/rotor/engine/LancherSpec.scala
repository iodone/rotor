package rotor.engine

/**
  * Created by iodone on {19-3-14}.
  */
import java.net.InetAddress

import com.typesafe.config.ConfigFactory
import org.mockito.Mockito._
import rotor.common.utils.Zookeeper


class LauncherSpec extends BaseTest {

  "Lancher" when {

    "Lancher#buildNewAkkaConf" should {
      "return new ip and port" in new Context {
        val localIP = InetAddress.getLocalHost.getHostAddress
        val address0 = List(s"${localIP}:30004", s"${localIP}:30003",s"${localIP}:30001", s"${localIP}:30000")
        val address1 = List(s"${localIP}:30002",s"${localIP}:30001", s"${localIP}:30000")
        val address2 = List(s"${localIP}:30002",s"${localIP}:30001")
        val address3 = List(s"127.0.1.1:30000",s"127.1.0.1:30000")
        val address4 = List()
        val address5 = List(s"${localIP}:30004",s"${localIP}:30003", s"${localIP}:30000")
        Launcher.genNewAddress(address0) shouldBe (localIP, "30002")
        Launcher.genNewAddress(address1) shouldBe (localIP, "30003")
        Launcher.genNewAddress(address2) shouldBe (localIP, "30000")
        Launcher.genNewAddress(address3) shouldBe (localIP, "30000")
        Launcher.genNewAddress(address4) shouldBe (localIP, "30000")
        Launcher.genNewAddress(address5) shouldBe (localIP, "30001")

      }
    }
  }

  private trait Context {
    val zkClient = mock[Zookeeper]
  }

}
