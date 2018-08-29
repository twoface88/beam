package beam.agentsim.agents.ridehail

import beam.agentsim.agents.GenericEventsSpec
import org.matsim.core.events.EventsUtils
import org.scalatest.{Ignore, Matchers}

@Ignore
class TNCIterationsStatsCollectorSpec extends GenericEventsSpec with Matchers {

  "A TNC Iterations Stats Collector " must {
    "collect stats" in {
      val events = EventsUtils.createEventsManager
      val tncHandler = new TNCIterationsStatsCollector(
        events,
        beamServices,
        null,
        networkCoordinator.transportNetwork
      )

      processHandlers(List(tncHandler))

      tncHandler.rideHailStats should not be empty

      //TODO: add value assertions
    }
  }
}
