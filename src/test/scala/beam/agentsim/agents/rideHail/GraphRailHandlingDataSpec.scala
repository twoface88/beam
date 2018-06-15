package beam.agentsim.agents.rideHail

import java.util
import java.util.{List, Map}

import beam.agentsim.agents.rideHail.GraphRailHandlingDataSpec.{
  RideHailingGeneralGraph,
  RideHailingWaitingGraph,
  RideHailingWaitingSingleGraph
}
import beam.agentsim.events.{ModeChoiceEvent, PathTraversalEvent}
import beam.analysis.plots.modality.RideHailDistanceRowModel
import beam.analysis.plots.{
  RideHailStats,
  RideHailingWaitingSingleStats,
  RideHailingWaitingStats
}
import beam.integration.IntegrationSpecCommon
import beam.router.r5.NetworkCoordinator
import beam.sim.config.{BeamConfig, MatSimBeamConfigBuilder}
import beam.sim.{BeamHelper, BeamServices}
import beam.utils.FileUtils
import com.google.inject.Provides
import org.matsim.api.core.v01.events.{Event, PersonEntersVehicleEvent}
import org.matsim.core.api.experimental.events.EventsManager
import org.matsim.core.controler.AbstractModule
import org.matsim.core.controler.events.IterationEndsEvent
import org.matsim.core.controler.listener.IterationEndsListener
import org.matsim.core.events.handler.BasicEventHandler
import org.matsim.core.scenario.{MutableScenario, ScenarioUtils}
import org.matsim.core.utils.collections.Tuple
import org.scalatest.{Matchers, WordSpecLike}

import scala.collection.JavaConverters._

object GraphRailHandlingDataSpec {
  class RideHailingWaitingSingleGraph(
      beamServices: BeamServices,
      waitingComp: RideHailingWaitingSingleStats.SingleStatsComputation)
      extends BasicEventHandler
      with IterationEndsListener {

    private val railHailingStat =
      new RideHailingWaitingSingleStats(beamServices, waitingComp)

    override def reset(iteration: Int): Unit = {
      railHailingStat.resetStats()
    }

    override def handleEvent(event: Event): Unit = {
      event match {
        case evn
            if evn.getEventType.equalsIgnoreCase(ModeChoiceEvent.EVENT_TYPE)
              || event.getEventType.equalsIgnoreCase(
                PersonEntersVehicleEvent.EVENT_TYPE) =>
          railHailingStat.processStats(event)
        case evn @ (_: ModeChoiceEvent | _: PersonEntersVehicleEvent) =>
          railHailingStat.processStats(evn)
        case _ =>
      }
      Unit
    }

    override def notifyIterationEnds(event: IterationEndsEvent): Unit = {
      railHailingStat.createGraph(event)
    }
  }

  class RideHailingWaitingGraph(
      waitingComp: RideHailingWaitingStats.WaitingStatsComputation)
      extends BasicEventHandler
      with IterationEndsListener {

    private val railHailingStat =
      new RideHailingWaitingStats(waitingComp)

    override def reset(iteration: Int): Unit = {
      railHailingStat.resetStats()
    }

    override def handleEvent(event: Event): Unit = {
      event match {
        case evn
            if evn.getEventType.equalsIgnoreCase(ModeChoiceEvent.EVENT_TYPE)
              || event.getEventType.equalsIgnoreCase(
                PersonEntersVehicleEvent.EVENT_TYPE) =>
          railHailingStat.processStats(event)
        case evn @ (_: ModeChoiceEvent | _: PersonEntersVehicleEvent) =>
          railHailingStat.processStats(evn)
        case _ =>
      }
      Unit
    }

    override def notifyIterationEnds(event: IterationEndsEvent): Unit = {
      railHailingStat.createGraph(event)
    }
  }

  class RideHailingGeneralGraph(waitingComp: RideHailStats.RailHailComputation)
      extends BasicEventHandler
      with IterationEndsListener {

    private val railHailingStat =
      new RideHailStats(waitingComp)

    override def reset(iteration: Int): Unit = {
      railHailingStat.resetStats()
    }

    override def handleEvent(event: Event): Unit = {
      event match {
        case evn
            if evn.getEventType.equalsIgnoreCase(
              PathTraversalEvent.EVENT_TYPE) =>
          railHailingStat.processStats(event)
        case evn: PathTraversalEvent =>
          railHailingStat.processStats(evn)
        case _ =>
      }
      Unit
    }

    override def notifyIterationEnds(event: IterationEndsEvent): Unit = {
      railHailingStat.createGraph(event)
    }
  }
}

class GraphRailHandlingDataSpec
    extends WordSpecLike
    with Matchers
    with BeamHelper
    with IntegrationSpecCommon {

  "Graph Collected Data" must {

    def initialSetup(childModule: AbstractModule): Unit = {
      val beamConfig = BeamConfig(baseConfig)
      val configBuilder = new MatSimBeamConfigBuilder(baseConfig)
      val matsimConfig = configBuilder.buildMatSamConf()
      matsimConfig.planCalcScore().setMemorizingExperiencedPlans(true)
      FileUtils.setConfigOutputFile(beamConfig, matsimConfig)

      val networkCoordinator = new NetworkCoordinator(beamConfig)
      networkCoordinator.loadNetwork()

      val scenario =
        ScenarioUtils.loadScenario(matsimConfig).asInstanceOf[MutableScenario]
      scenario.setNetwork(networkCoordinator.network)

      val injector = org.matsim.core.controler.Injector.createInjector(
        scenario.getConfig,
        module(baseConfig, scenario, networkCoordinator.transportNetwork),
        childModule)

      val beamServices: BeamServices =
        injector.getInstance(classOf[BeamServices])

      beamServices.controler.run()
    }

    "contains non empty waiting single stats" in {

      val waitingStat =
        new RideHailingWaitingSingleStats.SingleStatsComputation {
          override def compute(stat: util.Map[Integer, java.lang.Double])
            : Array[Array[Double]] = {
            val data = super.compute(stat)
            data.length should be > 0
            data.foldRight(0) { case (arr, acc) => acc + arr.length } should be > 0
            data
          }
        }

      initialSetup(new AbstractModule() {
        override def install(): Unit = {
          addControlerListenerBinding().to(
            classOf[RideHailingWaitingSingleGraph])
        }

        @Provides def provideGraph(
            beamServices: BeamServices,
            eventsManager: EventsManager): RideHailingWaitingSingleGraph = {
          val graph =
            new RideHailingWaitingSingleGraph(beamServices, waitingStat)
          eventsManager.addHandler(graph)
          graph
        }
      })
    }

    "contains non empty waiting stats" in {

      val waitingStat =
        new RideHailingWaitingStats.WaitingStatsComputation {
          override def compute(
              stat: Tuple[util.List[java.lang.Double],
                          util.Map[Integer, util.List[java.lang.Double]]])
            : util.Map[Integer, util.Map[java.lang.Double, Integer]] = {
            val data = super.compute(stat)
            val asScala = data.asScala
            asScala.isEmpty shouldBe false
            asScala.foldRight(0) { case (arr, acc) => acc + arr._2.size } should be > 0
            data
          }
        }

      initialSetup(new AbstractModule() {
        override def install(): Unit = {
          addControlerListenerBinding().to(classOf[RideHailingWaitingGraph])
        }

        @Provides def provideGraph(
            eventsManager: EventsManager): RideHailingWaitingGraph = {
          val graph =
            new RideHailingWaitingGraph(waitingStat)
          eventsManager.addHandler(graph)
          graph
        }
      })
    }
    "contains non empty general stats" in {

      val waitingStat =
        new RideHailStats.RailHailComputation {
          override def compute(
              stat: util.Map[String, util.List[PathTraversalEvent]])
            : util.Map[RideHailDistanceRowModel.GraphType, java.lang.Double] = {
            val data = super.compute(stat)
            data.asScala.isEmpty shouldBe false
            data
          }
        }

      initialSetup(new AbstractModule() {
        override def install(): Unit = {
          addControlerListenerBinding().to(classOf[RideHailingGeneralGraph])
        }

        @Provides def provideGraph(
            eventsManager: EventsManager): RideHailingGeneralGraph = {
          val graph =
            new RideHailingGeneralGraph(waitingStat)
          eventsManager.addHandler(graph)
          graph
        }
      })
    }
  }
}
