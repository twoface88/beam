package beam.agentsim.infrastructure

import java.util.concurrent.TimeUnit

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
import akka.util.Timeout
import beam.agentsim.infrastructure.ParkingManager.{
  DepotParkingInquiry,
  DepotParkingInquiryResponse,
  ParkingStockAttributes
}
import beam.sim.BeamServices
import beam.sim.common.GeoUtilsImpl
import beam.sim.config.BeamConfig
import beam.utils.TestConfigUtils.testConfig
import com.typesafe.config.{ConfigFactory, ConfigValueFactory}
import org.matsim.api.core.v01.Coord
import org.matsim.core.controler.MatsimServices
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, FunSpecLike, Ignore}

@Ignore
class ZonalParkingManagerSpec
    extends TestKit(
      ActorSystem(
        "testsystem",
        ConfigFactory.parseString("""
  akka.log-dead-letters = 10
  akka.actor.debug.fsm = true
  akka.loglevel = debug
  """).withFallback(testConfig("test/input/beamville/beam.conf"))
      )
    )
    with FunSpecLike
    with BeforeAndAfterAll
    with MockitoSugar
    with ImplicitSender {}
