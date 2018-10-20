package beam.sim;

import beam.agentsim.events.BeamEventsToLegs;
import com.google.inject.Inject;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.controler.ControlerListenerManager;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scoring.*;

import java.util.HashMap;
import java.util.Map;

/**
 * BEAM
 */
public class BeamExperiencedPlanServiceImpl implements ExperiencedPlansService, BeamEventsToLegs.LegHandler, EventsToActivities.ActivityHandler {

    private final static Logger log = Logger.getLogger(BeamExperiencedPlanServiceImpl.class);

    @Inject
    private Config config;
    @Inject private Population population;
    @Inject(optional = true) private ScoringFunctionsForPopulation scoringFunctionsForPopulation;

    private final Map<Id<Person>, Plan> agentRecords = new HashMap<>();

    @Inject
    BeamExperiencedPlanServiceImpl(ControlerListenerManager controlerListenerManager, EventsToActivities eventsToActivities, BeamEventsToLegs eventsToLegs) {
        controlerListenerManager.addControlerListener(new IterationStartsListener() {
            @Override
            public void notifyIterationStarts(IterationStartsEvent event) {
                for (Person person : population.getPersons().values()) {
                    agentRecords.put(person.getId(), PopulationUtils.createPlan());
                }
            }
        });
        eventsToActivities.addActivityHandler(this);
        eventsToLegs.addLegHandler(this);
    }

    @Override
    synchronized public void handleLeg(PersonExperiencedLeg o) {
        // Has to be synchronized because the thing which sends Legs and the thing which sends Activities can run
        // on different threads. Will go away when/if we get a more Actor or Reactive Streams like event infrastructure.
        Id<Person> agentId = o.getAgentId();
        Leg leg = o.getLeg();
        Plan plan = agentRecords.get(agentId);
        if (plan != null) {
            plan.addLeg(leg);
        }
    }

    @Override
    synchronized public void handleActivity(PersonExperiencedActivity o) {
        // Has to be synchronized because the thing which sends Legs and the thing which sends Activities can run
        // on different threads. Will go away when/if we get a more Actor or Reactive Streams like event infrastructure.
        Id<Person> agentId = o.getAgentId();
        Activity activity = o.getActivity();
        Plan plan = agentRecords.get(agentId);
        if (plan != null) {
            agentRecords.get(agentId).addActivity(activity);
        }
    }

    @Override
    public void writeExperiencedPlans(String iterationFilename) {
//		finishIteration(); // already called somewhere else in pgm flow.
        Population tmpPop = PopulationUtils.createPopulation(config);
        for (Map.Entry<Id<Person>, Plan> entry : this.agentRecords.entrySet()) {
            Person person = PopulationUtils.getFactory().createPerson(entry.getKey());
            Plan plan = entry.getValue();
            person.addPlan(plan);
            tmpPop.addPerson(person);
        }
        new PopulationWriter(tmpPop, null).write(iterationFilename);
        // I removed the "V5" here in the assumption that it is better to move along with future format changes.  If this is
        // undesired, please change back but could you then please also add a comment why you prefer this.  Thanks.
        // kai, jan'16
    }
    @Override
    public final void finishIteration() {
        // I separated this from "writeExperiencedPlans" so that it can be called separately even when nothing is written.  Can't say
        // if the design might be better served by an iteration ends listener.  kai, feb'17
        for (Map.Entry<Id<Person>, Plan> entry : this.agentRecords.entrySet()) {
            Plan plan = entry.getValue();
            if (scoringFunctionsForPopulation != null) {
                plan.setScore(scoringFunctionsForPopulation.getScoringFunctionForAgent(entry.getKey()).getScore());
                if (plan.getScore().isNaN()) {
                    log.warn("score is NaN; plan:" + plan.toString());
                }
            }
        }
    }

    @Override
    public Map<Id<Person>, Plan> getExperiencedPlans() {
        return this.agentRecords;
    }

}

