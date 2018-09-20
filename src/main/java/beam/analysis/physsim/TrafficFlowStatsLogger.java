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

import java.util.*;

public class TrafficFlowStatsLogger implements LinkEnterEventHandler, LinkLeaveEventHandler, VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler, BasicEventHandler{
    private final Network network;
    private Logger log = LoggerFactory.getLogger(TrafficFlowStatsLogger.class);
    HashMap<String, Double> linkEnterTime;
    HashMap<String, Id<Link>> vehicleEnteredLink;

    double totalTravelTime;
    double numberOfLinksTravelled;
    double totalDistanceTravelled;

    HashSet<Id<Link>> linkUsed;
    HashSet<String> vehiclesTravelled;
    private boolean isPhysSim;

    public TrafficFlowStatsLogger(EventsManager eventsManager, Network network, boolean isPhysSim){
        this.isPhysSim = isPhysSim;
        eventsManager.addHandler(this);
        this.network=network;
        reset(0);
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
        if (isRelevantVehicle(vehicleId)) {
            this.linkEnterTime.put(vehicleId, linkEnterTime);
            vehicleEnteredLink.put(vehicleId, linkId);
            update(linkId, vehicleId);
        }
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
        if (isRelevantVehicle(vehicleId) && linkEnterTime.containsKey(vehicleId)) { // e.g. vehicle might leave immediatly if destination on same link
            totalTravelTime += leaveTime - linkEnterTime.get(vehicleId);
            linkEnterTime.remove(vehicleId);
            numberOfLinksTravelled++;
            update(linkId,vehicleId);
        }
    }

    public void update(Id<Link> linkId,String vehicleId){
        if (linkId!=null) linkUsed.add(linkId);
        vehiclesTravelled.add(vehicleId);
    }

    public void logStats(String debugTag){
        log.info(debugTag);
        log.info("total car travel time (sec):" + totalTravelTime);
        log.info("number of links travelled:" + numberOfLinksTravelled);
        log.info("links touched:" + linkUsed.size());
        log.info("vehicles on road network:" + vehiclesTravelled.size());

        List list=new LinkedList(linkUsed);
        Collections.sort(list);
        log.info("links" + list);


        log.info("==============================");
    }

    private boolean isRelevantVehicle(String vehicleId){
        return isPhysSim || vehicleId.contains("rideHail") || vehicleId.contains("bus") || isPersonalCar(vehicleId);
    }





    @Override
    public void handleEvent(Event event) {
        if (event.getEventType().toString().equalsIgnoreCase("wait2link")){
            DebugLib.emptyFunctionForSettingBreakPoint();
        }
    }




    public static boolean isPersonalCar(String vehicleId) {
        return vehicleId.replaceAll("-","").matches("[0-9]+");
    }

    @Override
    public void reset(int iteration) {
        linkEnterTime=new HashMap<>();
        vehicleEnteredLink=new HashMap<>();
        totalTravelTime=0;
        numberOfLinksTravelled=0;
        totalDistanceTravelled=0;
        linkUsed=new HashSet<Id<Link>>();
        vehiclesTravelled=new HashSet<String>();
    }


}
