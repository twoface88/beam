package beam.sim;

import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.corelisteners.PlansScoring;

/**
 * BEAM
 */
public final class BeamPlansScoringModule extends AbstractModule {
    @Override
    public void install() {
//        bind(ScoringFunctionsForPopulation.class).asEagerSingleton();
        bind(PlansScoring.class).to(BeamPlanScoringImpl.class);
    }
}
