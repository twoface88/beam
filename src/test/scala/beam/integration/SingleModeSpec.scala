package beam.integration

import akka.actor._
import beam.agentsim.agents.PersonTestUtil
import beam.agentsim.agents.modalbehaviors.ModeChoiceCalculator
import beam.agentsim.agents.ridehail.{RideHailIterationHistory, RideHailSurgePricingManager}
import beam.agentsim.events.PathTraversalEvent
import beam.router.{BeamRouter, BeamSkimmer, RouteHistory}
import beam.router.Modes.BeamMode
import beam.sim.{BeamMobsim, BeamServices, BeamServicesImpl}
import beam.utils.SimRunnerForTest
import beam.utils.TestConfigUtils.testConfig
import com.typesafe.config.ConfigFactory
import org.matsim.api.core.v01.events.{ActivityEndEvent, Event, PersonDepartureEvent, PersonEntersVehicleEvent}
import org.matsim.api.core.v01.population.{Activity, Leg}
import org.matsim.core.events.handler.BasicEventHandler
import org.matsim.core.events.{EventsManagerImpl, EventsUtils}
import org.scalatest._
import org.scalatest.mockito.MockitoSugar

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.language.postfixOps

class SingleModeSpec
    extends SimRunnerForTest
    with WordSpecLike
    with Matchers
    with MockitoSugar
    with BeforeAndAfterEach {

  def config: com.typesafe.config.Config =
    ConfigFactory
      .parseString("""akka.test.timefactor = 10""")
      .withFallback(testConfig("test/input/sf-light/sf-light.conf").resolve())
  def outputDirPath = basePath + "/" + testOutputDir + "single-mode-test"

  var router: ActorRef = _
  var services: BeamServices = _
  var nextId: Int = 0
  var system: ActorSystem = _

  override def beforeEach: Unit = {
    // Create brand new Actor system every time (just to make sure that the same actor names can be reused)
    system = ActorSystem("single-mode-test-" + nextId, config)
    nextId += 1
    services = new BeamServicesImpl(injector)
    services.matsimServices = matsimSvc
    services.modeChoiceCalculatorFactory = ModeChoiceCalculator(
      services.beamConfig.beam.agentsim.agents.modalBehaviors.modeChoiceClass,
      services
    )

    scenario.getPopulation.getPersons.values.asScala
      .foreach(p => PersonTestUtil.putDefaultBeamAttributes(p, BeamMode.allModes))

    router = system.actorOf(
      BeamRouter.props(
        services,
        networkCoordinator.transportNetwork,
        networkCoordinator.network,
        scenario,
        new EventsManagerImpl(),
        scenario.getTransitVehicles,
        fareCalculator,
        tollCalculator
      ),
      "router"
    )
    services.beamRouter = router
  }

  override def afterEach: Unit = {
    system.terminate()
    router = null
    services = null
  }

  "The agentsim" must {
    "let everybody walk when their plan says so" in {
      scenario.getPopulation.getPersons
        .values()
        .forEach { person =>
          {
            person.getSelectedPlan.getPlanElements.asScala.collect {
              case leg: Leg =>
                leg.setMode("walk")
            }
          }
        }
      val events = mutable.ListBuffer[Event]()
      val eventsManager = EventsUtils.createEventsManager()
      eventsManager.addHandler(
        new BasicEventHandler {
          override def handleEvent(event: Event): Unit = {
            event match {
              case event: PersonDepartureEvent =>
                events += event
              case _ =>
            }
          }
        }
      )
      val mobsim = new BeamMobsim(
        services,
        networkCoordinator.transportNetwork,
        tollCalculator,
        scenario,
        eventsManager,
        system,
        new RideHailSurgePricingManager(services),
        new RideHailIterationHistory(),
        new RouteHistory(),
        new BeamSkimmer(services.beamConfig)
      )
      mobsim.run()
      events.foreach {
        case event: PersonDepartureEvent =>
          assert(
            event.getLegMode == "walk" || event.getLegMode == "be_a_tnc_driver" || event.getLegMode == "be_a_household_cav_driver" || event.getLegMode == "cav"
          )
      }
    }

    "let everybody take transit when their plan says so" in {
      scenario.getPopulation.getPersons
        .values()
        .forEach { person =>
          person.getSelectedPlan.getPlanElements.asScala.collect {
            case leg: Leg =>
              leg.setMode("walk_transit")
          }
        }
      val events = mutable.ListBuffer[Event]()
      val eventsManager = EventsUtils.createEventsManager()
      eventsManager.addHandler(
        new BasicEventHandler {
          override def handleEvent(event: Event): Unit = {
            event match {
              case event: PersonDepartureEvent =>
                events += event
              case _ =>
            }
          }
        }
      )
      val mobsim = new BeamMobsim(
        services,
        networkCoordinator.transportNetwork,
        tollCalculator,
        scenario,
        eventsManager,
        system,
        new RideHailSurgePricingManager(services),
        new RideHailIterationHistory(),
        new RouteHistory(),
        new BeamSkimmer(services.beamConfig)
      )
      mobsim.run()
      events.foreach {
        case event: PersonDepartureEvent =>
          assert(
            event.getLegMode == "walk" || event.getLegMode == "walk_transit" || event.getLegMode == "be_a_tnc_driver" || event.getLegMode == "be_a_household_cav_driver" || event.getLegMode == "cav"
          )
      }
    }

    "let everybody take drive_transit when their plan says so" in {
      // Here, we only set the mode for the first leg of each tour -- prescribing a mode for the tour,
      // but not for individual legs except the first one.
      // We want to make sure that our car is returned home.
      scenario.getPopulation.getPersons
        .values()
        .forEach { person =>
          {
            val newPlanElements = person.getSelectedPlan.getPlanElements.asScala.collect {
              case activity: Activity if activity.getType == "Home" =>
                Seq(activity, scenario.getPopulation.getFactory.createLeg("drive_transit"))
              case activity: Activity =>
                Seq(activity)
              case leg: Leg =>
                Nil
            }.flatten
            if (newPlanElements.last.isInstanceOf[Leg]) {
              newPlanElements.remove(newPlanElements.size - 1)
            }
            person.getSelectedPlan.getPlanElements.clear()
            newPlanElements.foreach {
              case activity: Activity =>
                person.getSelectedPlan.addActivity(activity)
              case leg: Leg =>
                person.getSelectedPlan.addLeg(leg)
            }
          }
        }
      val events = mutable.ListBuffer[Event]()
      val eventsManager = EventsUtils.createEventsManager()
      eventsManager.addHandler(
        new BasicEventHandler {
          override def handleEvent(event: Event): Unit = {
            event match {
              case event @ (_: PersonDepartureEvent | _: ActivityEndEvent) =>
                events += event
              case _ =>
            }
          }
        }
      )
      val mobsim = new BeamMobsim(
        services,
        networkCoordinator.transportNetwork,
        tollCalculator,
        scenario,
        eventsManager,
        system,
        new RideHailSurgePricingManager(services),
        new RideHailIterationHistory(),
        new RouteHistory,
        new BeamSkimmer(services.beamConfig)
      )
      mobsim.run()
      events.collect {
        case event: PersonDepartureEvent =>
          // drive_transit can fail -- maybe I don't have a car
          assert(
            event.getLegMode == "walk" || event.getLegMode == "walk_transit" || event.getLegMode == "drive_transit" || event.getLegMode == "be_a_tnc_driver" || event.getLegMode == "be_a_household_cav_driver" || event.getLegMode == "cav"
          )
      }
      val eventsByPerson = events.groupBy(_.getAttributes.get("person"))
      val filteredEventsByPerson = eventsByPerson.filter {
        _._2
          .filter(_.isInstanceOf[ActivityEndEvent])
          .sliding(2)
          .exists(
            pair => pair.forall(activity => activity.asInstanceOf[ActivityEndEvent].getActType != "Home")
          )
      }
      eventsByPerson.map {
        _._2.span {
          case event: ActivityEndEvent if event.getActType == "Home" =>
            true
          case _ =>
            false
        }
      }
      // TODO: Test that what can be printed with the line below makes sense (chains of modes)
      //      filteredEventsByPerson.map(_._2.mkString("--\n","\n","--\n")).foreach(print(_))
    }

    "let everybody drive when their plan says so" in {
      scenario.getPopulation.getPersons
        .values()
        .forEach { person =>
          {
            person.getSelectedPlan.getPlanElements.asScala.collect {
              case leg: Leg =>
                leg.setMode("car")
            }
          }
        }
      val eventsManager = EventsUtils.createEventsManager()
      val events = mutable.ListBuffer[Event]()
      eventsManager.addHandler(
        new BasicEventHandler {
          override def handleEvent(event: Event): Unit = {
            event match {
              case event @ (_: PersonDepartureEvent | _: ActivityEndEvent | _: PathTraversalEvent |
                  _: PersonEntersVehicleEvent) =>
                events += event
              case _ =>
            }
          }
        }
      )

      val mobsim = new BeamMobsim(
        services,
        networkCoordinator.transportNetwork,
        tollCalculator,
        scenario,
        eventsManager,
        system,
        new RideHailSurgePricingManager(services),
        new RideHailIterationHistory(),
        new RouteHistory(),
        new BeamSkimmer(services.beamConfig)
      )
      mobsim.run()
      events.collect {
        case event: PersonDepartureEvent =>
          // Wr still get some failing car routes.
          // TODO: Find root cause, fix, and remove "walk" here.
          // See SfLightRouterSpec.
          assert(
            event.getLegMode == "walk" || event.getLegMode == "car" || event.getLegMode == "be_a_tnc_driver" || event.getLegMode == "be_a_household_cav_driver" || event.getLegMode == "cav"
          )
      }
    }
  }

}
