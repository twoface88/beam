package beam.agentsim.agents.rideHail.allocationManagers

import beam.agentsim.agents.rideHail.{RideHailingManager, TNCIterationStats}
import beam.router.BeamRouter.Location
import org.matsim.api.core.v01.{Coord, Id}
import org.matsim.vehicles.Vehicle

class RepositioningWithLowWaitingTimes(val rideHailingManager: RideHailingManager, val tncIterationStats: Option[TNCIterationStats]) extends RideHailResourceAllocationManager {

  val isBufferedRideHailAllocationMode = false

  def proposeVehicleAllocation(vehicleAllocationRequest: VehicleAllocationRequest): Option[VehicleAllocation] = {
    None
  }

  def allocateVehicles(allocationsDuringReservation: Vector[(VehicleAllocationRequest, Option[VehicleAllocation])]): Vector[(VehicleAllocationRequest, Option[VehicleAllocation])] = {
    log.error("batch processing is not implemented for DefaultRideHailResourceAllocationManager")
    allocationsDuringReservation
  }

  def repositionVehicles(tick: Double): Vector[(Id[Vehicle], Location)] = {

    /*
    -> which tnc to reposition?
      -> go through all idle tncs
      -> if taxi

      -> find areas with taxis with long idle time
        -> threshhold parameter min idle time and max share to reposition

      -> ide now + idle for a longer time

    -> 	Randomly sampling (This is what we implement first)
		-> search radius: idle time in that area make as window (TAZ areas you can reach in 10min)
		-> sum up the demand (number of requests + waiting time + idle time)
			-> draw circle
			-> negative weight
			-> on TAZ
			-> sum per TAZ and time slot.

			=> talk at Matsim wrkshop: one without distribution, random, this method.
				=> demonstrate

				look at demand in 10min at the TAZ around me

				probabilityOfServing(taz_i) = score(taz_i) / sumOfScores
				score(taz_i) = alpha * demand + betta * waitingTimes
     */

    tncIterationStats match {
      case Some(stats) =>
        // iteration >0
        rideHailingManager.getIdleVehicles().values map { v =>
          val id: Id[Vehicle] = v.vehicleId
          val coord: Coord = v.currentLocation.loc

          val tazId: String = stats.tazTreeMap.getTAZ(coord.getX, coord.getY).tazId.toString

          val sumIV: Double = stats.getRideHailStatsInfo(coord, tick).get.sumOfIdlingVehicles
          val sumRR: Double = stats.getRideHailStatsInfo(coord, tick).get.sumOfRequestedRides
          val sumWT: Double = stats.getRideHailStatsInfo(coord, tick).get.sumOfWaitingtimes

          ()

          // TODO how to access ride requests data from this method?
          // TODO will target location be set as pickup location of specific ride request?
        }

      case None =>
        // iteration 0
    }

    Vector.empty[(Id[Vehicle], Location)]
  }
}
