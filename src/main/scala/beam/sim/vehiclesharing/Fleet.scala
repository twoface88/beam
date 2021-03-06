package beam.sim.vehiclesharing
import akka.actor.{ActorRef, Props}
import beam.agentsim.agents.Population
import beam.agentsim.agents.vehicles.BeamVehicleType
import beam.sim.BeamServices
import beam.sim.config.BeamConfig.Beam.Agentsim.Agents.Vehicles.SharedFleets$Elm
import org.matsim.api.core.v01.{Id, Scenario}

import scala.collection.JavaConverters._

trait FleetType {
  def props(beamServices: BeamServices, parkingManager: ActorRef): Props
}

case class FixedNonReservingFleet(config: SharedFleets$Elm.FixedNonReserving) extends FleetType {
  override def props(beamServices: BeamServices, parkingManager: ActorRef): Props = {
    val initialSharedVehicleLocations =
      beamServices.matsimServices.getScenario.getPopulation.getPersons
        .values()
        .asScala
        .map(Population.personInitialLocation)
    val vehicleType = beamServices.vehicleTypes.getOrElse(
      Id.create(config.vehicleTypeId, classOf[BeamVehicleType]),
      throw new RuntimeException("Vehicle type id not found: " + config.vehicleTypeId)
    )
    Props(new FixedNonReservingFleetManager(parkingManager, initialSharedVehicleLocations, vehicleType))
  }
}

case class InexhaustibleReservingFleet(config: SharedFleets$Elm.InexhaustibleReserving) extends FleetType {
  override def props(beamServices: BeamServices, parkingManager: ActorRef): Props = {
    val vehicleType = beamServices.vehicleTypes.getOrElse(
      Id.create(config.vehicleTypeId, classOf[BeamVehicleType]),
      throw new RuntimeException("Vehicle type id not found: " + config.vehicleTypeId)
    )
    Props(new InexhaustibleReservingFleetManager(parkingManager, vehicleType))
  }
}
