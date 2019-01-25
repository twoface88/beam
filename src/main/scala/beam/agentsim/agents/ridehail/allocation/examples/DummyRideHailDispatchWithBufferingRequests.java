package beam.agentsim.agents.ridehail.allocation.examples;

import beam.agentsim.agents.ridehail.RideHailManager;
import beam.agentsim.agents.ridehail.allocation.RideHailResourceAllocationManager;

public class DummyRideHailDispatchWithBufferingRequests extends RideHailResourceAllocationManager {

    public DummyRideHailDispatchWithBufferingRequests(RideHailManager rideHailManager) {
        super(rideHailManager);
    }
}
