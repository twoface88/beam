package beam.sim;

import org.matsim.core.controler.AbstractModule;
import org.matsim.core.scoring.ExperiencedPlansService;

/**
 * BEAM
 */
public final class BeamExperiencedPlansModule extends AbstractModule {
    @Override
    public void install() {
        install(new BeamExperiencedPlanElementsModule());
        bind(ExperiencedPlansService.class).to(BeamExperiencedPlanServiceImpl.class).asEagerSingleton();
    }
}

