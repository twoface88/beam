package beam.sim;

import beam.agentsim.events.BeamEventsToLegs;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.scoring.EventsToActivities;
import org.matsim.core.scoring.EventsToLegs;

/**
 * BEAM
 */
public final class BeamExperiencedPlanElementsModule extends AbstractModule {
    @Override
    public void install() {
        bind(EventsToActivities.class).asEagerSingleton();
        bind(BeamEventsToLegs.class).asEagerSingleton();
    }
}

