package beam.agentsim.agents.ridehail.allocation.examples

import beam.agentsim.agents.ridehail.RideHailManager
import beam.agentsim.agents.ridehail.allocation.RideHailResourceAllocationManager

class DummyRideHailDispatchWithBufferingRequestsWithoutReplacement(val rideHailManager: RideHailManager) extends RideHailResourceAllocationManager(rideHailManager) {}

