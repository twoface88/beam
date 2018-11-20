package beam.integration

import java.io.File
import java.util.zip.ZipFile

import beam.agentsim.events.PathTraversalEvent
import beam.sim.BeamHelper
import beam.sim.config.BeamConfig
import com.typesafe.config.{Config, ConfigValueFactory}
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent
import org.matsim.api.core.v01.population.{Activity, Leg}
import org.matsim.core.config.ConfigUtils
import org.matsim.core.population.io.PopulationReader
import org.matsim.core.population.routes.NetworkRoute
import org.matsim.core.scenario.ScenarioUtils
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

import scala.collection.JavaConverters._

/**
  * Created by fdariasm on 29/08/2017
  *
  */
class EventsFileSpec
    extends FlatSpec
    with BeforeAndAfterAll
    with Matchers
    with BeamHelper
    with EventsFileHandlingCommon
    with IntegrationSpecCommon {

  private lazy val config: Config = baseConfig
    .withValue("beam.outputs.events.fileOutputFormats", ConfigValueFactory.fromAnyRef("xml,csv"))
    .withValue("beam.routing.transitOnStreetNetwork", ConfigValueFactory.fromAnyRef("true"))
    .resolve()

  lazy val beamConfig = BeamConfig(config)
  var matsimConfig: org.matsim.core.config.Config = _

  override protected def beforeAll(): Unit = {
    matsimConfig = runBeamWithConfig(config)._1
  }

  // TODO: probably test needs to be updated due to update in rideHailManager
  it should "contain all bus routes" in {

    val zipFile = new ZipFile("test/input/beamville/r5/bus.zip")
    val tripsEntry = zipFile.getInputStream(zipFile.getEntry("trips.txt"))
    val listTrips =
      getListIDsWithTag(tripsEntry, "route_id", 2).sorted

    val reader = new ReadEventsBeam()
    val events = reader.readEvents(getEventsFilePath(matsimConfig, "xml").getAbsolutePath)

    val transitDriverAgentBusEvents = events.filter{ e =>
      PersonEntersVehicleEvent.EVENT_TYPE.equals(e.getEventType) &&
      Option(e.getAttributes.get("person")).exists(_.contains("TransitDriverAgent-bus:"))
    }
    transitDriverAgentBusEvents.size shouldEqual listTrips.size
  }

  it should "contain all train routes" in {

    val zipFile = new ZipFile("test/input/beamville/r5/train.zip")
    val tripsEntry = zipFile.getInputStream(zipFile.getEntry("trips.txt"))

    val listTrips =
      getListIDsWithTag(tripsEntry, "route_id", 2).sorted

    val reader = new ReadEventsBeam()
    val events = reader.readEvents(getEventsFilePath(matsimConfig, "xml").getAbsolutePath)

    val transitDriverAgentTrainEvents = events.filter{ e =>
      PersonEntersVehicleEvent.EVENT_TYPE.equals(e.getEventType) &&
        Option(e.getAttributes.get("person")).exists(_.contains("TransitDriverAgent-train:"))
    }

    val trainVehiclesFromEvent = transitDriverAgentTrainEvents.map{ e =>
      e.getAttributes.get("vehicle")
    }

    listTrips.forall(e => trainVehiclesFromEvent.exists(_.contains(e))) shouldBe true

//    transitDriverAgentTrainEvents.size shouldEqual listTrips.size
  }

  it should "contain the same bus trips entries" in {

    val zipFile = new ZipFile("test/input/beamville/r5/bus.zip")
    val tripsEntry = zipFile.getInputStream(zipFile.getEntry("trips.txt"))

    val listTrips =
      getListIDsWithTag(tripsEntry, "route_id", 2).sorted

    val reader = new ReadEventsBeam()
    val events = reader.readEvents(getEventsFilePath(matsimConfig, "xml").getAbsolutePath)

    val transitDriverAgentBusEvents = events.filter{ e =>
      PersonEntersVehicleEvent.EVENT_TYPE.equals(e.getEventType) &&
        Option(e.getAttributes.get("person")).exists(_.contains("TransitDriverAgent-bus:"))
    }

    val vehicles = transitDriverAgentBusEvents.map{ e =>
      e.getAttributes
        .get("vehicle")
        .split(":")(1)
    }.sorted

    vehicles shouldBe listTrips
  }

  it should "contain the same train trips entries" in {
    val zipFile = new ZipFile("test/input/beamville/r5/train.zip")
    val tripsEntry = zipFile.getInputStream(zipFile.getEntry("trips.txt"))

    val listTrips =
      getListIDsWithTag(tripsEntry, "route_id", 2).sorted

    val reader = new ReadEventsBeam()
    val events = reader.readEvents(getEventsFilePath(matsimConfig, "xml").getAbsolutePath)

    val transitDriverAgentBusEvents = events.filter{ e =>
      PersonEntersVehicleEvent.EVENT_TYPE.equals(e.getEventType) &&
        Option(e.getAttributes.get("person")).exists(_.contains("TransitDriverAgent-train:"))
    }

    val vehicles = transitDriverAgentBusEvents.map{ e =>
      e.getAttributes
        .get("vehicle")
        .split(":")(1)
    }.sorted

    listTrips.forall(s => vehicles.exists(_.contains(s)))

//    vehicles shouldBe listTrips
  }

  it should "contain same pathTraversal defined at stop times file for bus input file" in {

    val zipFile = new ZipFile("test/input/beamville/r5/bus.zip")
    val tripsEntry = zipFile.getInputStream(zipFile.getEntry("stop_times.txt"))
    val listTrips =
      getListIDsWithTag(tripsEntry, "trip_id", 0).sorted

    val grouped = listTrips.groupBy(identity)
    val groupedWithCount = grouped.map { case (k, v) => (k, v.size - 1) }

    val reader = new ReadEventsBeam()
    val events = reader.readEvents(getEventsFilePath(matsimConfig, "xml").getAbsolutePath)

    val transitDriverAgentBusEvents = events.filter{ e =>
      PathTraversalEvent.EVENT_TYPE.equals(e.getEventType) &&
        Option(e.getAttributes.get("vehicle")).exists(_.contains("bus:"))
    }
    val vehicles = transitDriverAgentBusEvents.map{ e =>
      e.getAttributes
        .get("vehicle")
        .split(":")(1)
    }.sorted

    val groupedXml = vehicles.groupBy(identity)
    val groupedXmlWithCount = groupedXml.map { case (k, v) => (k, v.size) }

    groupedXmlWithCount should contain theSameElementsAs groupedWithCount
  }

  it should "contain same pathTraversal defined at stop times file for train input file" in {

    val zipFile = new ZipFile("test/input/beamville/r5/train.zip")
    val tripsEntry = zipFile.getInputStream(zipFile.getEntry("stop_times.txt"))
    val listTrips = getListIDsWithTag(
      tripsEntry,
      "trip_id",
      0
    ).sorted
    val grouped = listTrips.groupBy(identity)
    val groupedWithCount = grouped.map { case (k, v) => (k, v.size - 1) }

    val reader = new ReadEventsBeam()
    val events = reader.readEvents(getEventsFilePath(matsimConfig, "xml").getAbsolutePath)

    val transitDriverAgentBusEvents = events.filter{ e =>
      PathTraversalEvent.EVENT_TYPE.equals(e.getEventType) &&
        Option(e.getAttributes.get("vehicle")).exists(_.contains("train:"))
    }
    val vehicles = transitDriverAgentBusEvents.map{ e =>
      e.getAttributes
        .get("vehicle")
        .split(":")(1)
    }.sorted

    val groupedXml = vehicles.groupBy(identity)
    val groupedXmlWithCount = groupedXml.map { case (k, v) => (k, v.size) }

    groupedXmlWithCount should contain theSameElementsAs groupedWithCount
  }

  it should "be available as csv file" in {
    assert(getEventsFilePath(matsimConfig, "csv").exists())
  }

  it should "produce experienced plans which make sense" in {
    val scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig())
    new PopulationReader(scenario).readFile(
      s"${matsimConfig.controler().getOutputDirectory}/ITERS/it.0/0.experiencedPlans.xml.gz"
    )
    assert(scenario.getPopulation.getPersons.size() == 50)
    scenario.getPopulation.getPersons.values().forEach { person =>
      val experiencedPlan = person.getPlans.get(0)
      assert(experiencedPlan.getPlanElements.size() > 1)
      experiencedPlan.getPlanElements.asScala.sliding(2).foreach {
        case Seq(activity: Activity, leg: Leg) =>
          assert(activity.getEndTime == leg.getDepartureTime)
        case Seq(leg: Leg, activity: Activity) =>
          assert(leg.getDepartureTime + leg.getTravelTime == activity.getStartTime)
          if (leg.getMode == "car") {
            assert(leg.getRoute.isInstanceOf[NetworkRoute])
          }
      }
    }
  }

}
