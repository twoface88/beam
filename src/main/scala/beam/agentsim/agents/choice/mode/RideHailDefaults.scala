package beam.agentsim.agents.choice.mode

import beam.router.Modes.BeamMode
import beam.router.Modes.BeamMode.{BUS, CAR, FERRY, RAIL, RIDE_HAIL, RIDE_HAIL_TRANSIT, SUBWAY, TRANSIT}
import beam.router.RoutingModel.EmbodiedBeamTrip
import beam.sim.BeamServices
import org.matsim.api.core.v01.Id

/**
  * RideHailDefaults
  *
  * If no fare is found, these defaults can be use.
  */
object RideHailDefaults{
  val DEFAULT_COST_PER_MILE = 2.00

  def estimateRideHailCost(alternatives: Seq[EmbodiedBeamTrip]) = {
    alternatives.map{ alt =>
      alt.tripClassifier match {
        case RIDE_HAIL if alt.costEstimate == 0.0 =>
          val cost = alt.legs.filter(_.beamLeg.mode == CAR).map(_.beamLeg.travelPath.distanceInM).sum * DEFAULT_COST_PER_MILE / 1607
          BigDecimal(cost)
        case RIDE_HAIL_TRANSIT if alt.costEstimate == 0.0 =>
          val cost = alt.legs.filter(_.beamLeg.mode == CAR).map(_.beamLeg.travelPath.distanceInM).sum * DEFAULT_COST_PER_MILE / 1607
          BigDecimal(cost)
        case _ =>
          BigDecimal(0)
      }
    }
  }

}