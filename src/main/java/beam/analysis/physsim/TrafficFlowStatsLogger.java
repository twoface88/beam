package beam.analysis.physsim;

import beam.utils.DebugLib;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.handler.BasicEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

public class TrafficFlowStatsLogger implements LinkEnterEventHandler, LinkLeaveEventHandler, VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler, BasicEventHandler{
    private final Network network;
    private Logger log = LoggerFactory.getLogger(TrafficFlowStatsLogger.class);
    HashMap<String, Double> linkEnterTime=new HashMap<>();
    HashMap<String, Id<Link>> vehicleEnteredLink=new HashMap<>();

    double totalTravelTime=0;
    double numberOfLinksTravelled=0;
    double totalDistanceTravelled=0;

    HashSet<Id<Link>> linkUsed=new HashSet<Id<Link>>();
    HashSet<String> vehiclesTravelled=new HashSet<String>();

    public TrafficFlowStatsLogger(EventsManager eventsManager, Network network){
        eventsManager.addHandler(this);
        this.network=network;
    }

    @Override
    public void handleEvent(VehicleEntersTrafficEvent event) {
        enterLink(event.getLinkId(),event.getVehicleId().toString(),event.getTime());
    }

    @Override
    public void handleEvent(LinkEnterEvent event) {
        enterLink(event.getLinkId(),event.getVehicleId().toString(),event.getTime());
    }

    private void enterLink(Id<Link> linkId, String vehicleId, double linkEnterTime){
        this.linkEnterTime.put(vehicleId,linkEnterTime);
        vehicleEnteredLink.put(vehicleId,linkId);
        update(linkId,vehicleId);
    }

    @Override
    public void handleEvent(LinkLeaveEvent event) {
        leaveLink(event.getLinkId(),event.getVehicleId().toString(),event.getTime());
    }

    @Override
    public void handleEvent(VehicleLeavesTrafficEvent event) {
        leaveLink(event.getLinkId()==null?vehicleEnteredLink.get(event.getVehicleId().toString()):event.getLinkId(),event.getVehicleId().toString(),event.getTime());
    }

    private void leaveLink(Id<Link> linkId,String vehicleId,double leaveTime){
        totalTravelTime+=leaveTime-linkEnterTime.get(vehicleId);
        linkEnterTime.remove(vehicleId);
        numberOfLinksTravelled++;
    }

    public void update(Id<Link> linkId,String vehicleId){
        linkUsed.add(linkId);
        vehiclesTravelled.add(vehicleId);
    }

    public void logStats(String debugTag){
        log.info(debugTag);
        log.info("total car travel time (sec):" + totalTravelTime);
        log.info("number of links travelled:" + numberOfLinksTravelled);
        log.info("used links:" + linkUsed.size());
        log.info("vehicles on road network:" + vehiclesTravelled.size());
    }


    @Override
    public void handleEvent(Event event) {
        if (event.getEventType().toString().equalsIgnoreCase("wait2link")){
            DebugLib.emptyFunctionForSettingBreakPoint();
        }
    }

}
