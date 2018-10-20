package beam.agentsim.events;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.api.experimental.events.handler.TeleportationArrivalEventHandler;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scoring.PersonExperiencedLeg;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

/**
 *
 * Converts a stream of Events into a stream of Legs. Passes Legs to a single LegHandler which must be registered with this class.
 * Mainly intended for scoring, but can be used for any kind of Leg related statistics. Essentially, it allows you to read
 * Legs from the simulation like you would read Legs from Plans, except that the Plan does not even need to exist.
 *
 * Note that the instances of Leg passed to the LegHandler will never be identical to those in the Scenario! Even
 * in a "no-op" simulation which only reproduces the Plan, new instances will be created. So if you attach your own data
 * to the Legs in the Scenario, that's your own lookout.
 *
 * @author michaz
 *
 */
public class BeamEventsToLegs implements PersonDepartureEventHandler, PersonArrivalEventHandler, LinkLeaveEventHandler, LinkEnterEventHandler,
        TeleportationArrivalEventHandler, VehicleLeavesTrafficEventHandler, VehicleEntersTrafficEventHandler {

    private Vehicle2DriverEventHandler delegate = new Vehicle2DriverEventHandler();

    public interface LegHandler {
        void handleLeg(PersonExperiencedLeg leg);
    }

    private Network network;

    @Inject(optional=true)
    private Map<Id<Person>, Leg> legs = new HashMap<>();
    private Map<Id<Person>, List<Id<Link>>> experiencedRoutes = new HashMap<>();
    private Map<Id<Person>, Double> relPosOnDepartureLinkPerPerson = new HashMap<>();
    private Map<Id<Person>, Double> relPosOnArrivalLinkPerPerson = new HashMap<>();
    private Map<Id<Person>, TeleportationArrivalEvent> routelessTravels = new HashMap<>();
    private List<LegHandler> legHandlers = new ArrayList<>();


    @Inject
    BeamEventsToLegs(Network network, EventsManager eventsManager) {
        this.network = network;
        eventsManager.addHandler(this);
    }

    public BeamEventsToLegs(Scenario scenario) {
        this.network = scenario.getNetwork();
    }

    @Override
    public void reset(int iteration) {
        legs.clear();
        experiencedRoutes.clear();
        routelessTravels.clear();
        delegate.reset(iteration);
    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {
        Leg leg = PopulationUtils.createLeg(event.getLegMode());
        leg.setDepartureTime(event.getTime());
        legs.put(event.getPersonId(), leg);

        List<Id<Link>> route = new ArrayList<>();
        route.add(event.getLinkId());
        experiencedRoutes.put(event.getPersonId(), route);
    }

    @Override
    public void handleEvent(LinkLeaveEvent event) {

    }

    @Override
    public void handleEvent(LinkEnterEvent event) {
        Id<Person> driverOfVehicle = delegate.getDriverOfVehicle(event.getVehicleId());
        List<Id<Link>> route = experiencedRoutes.get(driverOfVehicle);
        route.add(event.getLinkId());
    }

    @Override
    public void handleEvent(TeleportationArrivalEvent travelEvent) {
        routelessTravels.put(travelEvent.getPersonId(), travelEvent);
    }

    @Override
    public void handleEvent(PersonArrivalEvent event) {
        Leg leg = legs.get(event.getPersonId());
        leg.setTravelTime( event.getTime() - leg.getDepartureTime() );
        double travelTime = leg.getDepartureTime() + leg.getTravelTime() - leg.getDepartureTime();
        leg.setTravelTime(travelTime);
        List<Id<Link>> experiencedRoute = experiencedRoutes.get(event.getPersonId());
        assert experiencedRoute.size() >= 1  ;
        if (experiencedRoute.size() > 1) { // different links processed
            NetworkRoute networkRoute = RouteUtils.createNetworkRoute(experiencedRoute, null);
            networkRoute.setTravelTime(travelTime);

            /* use the relative position of vehicle enter/leave traffic events on first/last links
             * to calculate the correct route distance including the first/last link.
             * (see MATSIM-227) tt feb'16
             */
            double relPosOnDepartureLink = relPosOnDepartureLinkPerPerson.get(event.getPersonId());
            Double relPosOnArrivalLink = relPosOnArrivalLinkPerPerson.get(event.getPersonId());
            Gbl.assertNotNull( relPosOnArrivalLink );
            networkRoute.setDistance(RouteUtils.calcDistance(networkRoute, relPosOnDepartureLink,
                    relPosOnArrivalLink, network));

            leg.setRoute(networkRoute);
        } else {
            // i.e. experiencedRoute.size()==1 and no pendingTransitTravel

            TeleportationArrivalEvent travelEvent = routelessTravels.remove(event.getPersonId());
            Route genericRoute = RouteUtils.createGenericRouteImpl(experiencedRoute.get(0), event.getLinkId());
            genericRoute.setTravelTime(travelTime);
            if (travelEvent != null) {
                genericRoute.setDistance(travelEvent.getDistance());
            } else {
                genericRoute.setDistance(0.0);
            }
            leg.setRoute(genericRoute);
        }
        for (LegHandler legHandler : legHandlers) {
            legHandler.handleLeg(new PersonExperiencedLeg(event.getPersonId(), leg));
        }
    }

    public void addLegHandler(LegHandler legHandler) {
        this.legHandlers.add(legHandler);
    }

    @Override
    public void handleEvent(VehicleEntersTrafficEvent event) {
        delegate.handleEvent(event);

        // remember the relative position on the link
        relPosOnDepartureLinkPerPerson.put(event.getPersonId(), event.getRelativePositionOnLink());
    }

    @Override
    public void handleEvent(VehicleLeavesTrafficEvent event) {
        delegate.handleEvent(event);

        // remember the relative position on the link
        relPosOnArrivalLinkPerPerson.put(event.getPersonId(), event.getRelativePositionOnLink());
    }

}

