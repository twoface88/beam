package beam.utils

import beam.agentsim.agents.vehicles.BeamVehicleType._
import beam.agentsim.agents.vehicles.EnergyEconomyAttributes.Powertrain
import beam.agentsim.agents.vehicles.{BeamVehicle, BeamVehicleType}
import org.matsim.api.core.v01.Id
import org.matsim.vehicles.EngineInformation.FuelType
import org.matsim.vehicles._

import scala.collection.JavaConverters
import scala.collection.concurrent.TrieMap

object BeamVehicleUtils {

  def makeBicycle(id: Id[Vehicle]): BeamVehicle = {
    //FIXME: Every person gets a Bicycle (for now, 5/2018)

    val bvt = BeamVehicleType.defaultBicycleBeamVehicleType
    val beamVehicleId = BeamVehicle.createId(id, Some("bike"))
    val powertrain = Option(bvt.primaryFuelConsumptionInJoulePerMeter)
      .map(new Powertrain(_))
      .getOrElse(Powertrain.PowertrainFromMilesPerGallon(Powertrain.AverageMilesPerGallon))
    new BeamVehicle(
      beamVehicleId,
      powertrain,
      None,
      bvt
    )
  }

  //TODO: Identify the vehicles by type in xml
  def makeHouseholdVehicle(
                            beamVehicles: TrieMap[Id[BeamVehicle], BeamVehicle],
                            id: Id[Vehicle]
                          ): Either[IllegalArgumentException, BeamVehicle] = {

    if (BeamVehicleType.isBicycleVehicle(id)) {
      Right(makeBicycle(id))
    } else {
      beamVehicles
        .get(id)
        .toRight(
          new IllegalArgumentException(s"Invalid vehicle id $id")
        )
    }
  }

  def getVehicleTypeById(
                          id: String,
                          vehicleTypes: java.util.Map[Id[VehicleType], VehicleType]
                        ): Option[VehicleType] = {
    JavaConverters
      .mapAsScalaMap(vehicleTypes)
      .filter(idAndType => idAndType._2.getId.toString.equalsIgnoreCase(id))
      .values
      .headOption
  }

  def getVehicleTypeByDescription(
                                   description: String,
                                   vehicleTypes: java.util.Map[Id[VehicleType], VehicleType]
                                 ): Option[VehicleType] = {
    JavaConverters
      .mapAsScalaMap(vehicleTypes)
      .filter(idAndType => idAndType._2.getDescription.equalsIgnoreCase(description))
      .values
      .headOption
  }

  def beamFuelTypeToMatsimEngineInfo(beamVehicleType: BeamVehicleType): EngineInformationImpl = {
    val fuelConsumptionInJoulePerMeter = beamVehicleType.primaryFuelConsumptionInJoulePerMeter
    beamVehicleType.primaryFuelType.fuelTypeId match {
      case Biodiesel =>
        new EngineInformationImpl(FuelType.biodiesel, fuelConsumptionInJoulePerMeter * 1 / BIODIESEL_JOULE_PER_LITER)
      case Diesel => new EngineInformationImpl(FuelType.diesel, fuelConsumptionInJoulePerMeter * 1 / DIESEL_JOULE_PER_LITER)
      case Gasoline => new EngineInformationImpl(FuelType.gasoline, fuelConsumptionInJoulePerMeter * 1 / GASOLINE_JOULE_PER_LITER)
      case Electricity => new EngineInformationImpl(FuelType.electricity, fuelConsumptionInJoulePerMeter * 1 / ELECTRICITY_JOULE_PER_LITER)
      case _ => new EngineInformationImpl(FuelType.gasoline, fuelConsumptionInJoulePerMeter * 1 / GASOLINE_JOULE_PER_LITER)
    }
  }


  // From https://www.extension.iastate.edu/agdm/wholefarm/pdf/c6-87.pdf
  val GASOLINE_JOULE_PER_LITER = 34.8E6
  val DIESEL_JOULE_PER_LITER = 38.7E6
  val BIODIESEL_JOULE_PER_LITER = 35.2E6
  val ELECTRICITY_JOULE_PER_LITER = 1


  def beamVehicleTypeToMatsimVehicleType(beamVehicleType: BeamVehicleType): VehicleType = {
    val matsimVehicleType = VehicleUtils.getFactory.createVehicleType(Id.create(beamVehicleType.vehicleTypeId.toString, classOf[VehicleType]))

    val vehicleCapacity = new VehicleCapacityImpl()
    vehicleCapacity.setSeats(beamVehicleType.seatingCapacity)
    vehicleCapacity.setStandingRoom(beamVehicleType.standingRoomCapacity)
    matsimVehicleType.setCapacity(vehicleCapacity)

    val engineInformation = beamFuelTypeToMatsimEngineInfo(beamVehicleType)
    matsimVehicleType.setEngineInformation(engineInformation)

    matsimVehicleType.setLength(beamVehicleType.lengthInMeter)
    matsimVehicleType.setPcuEquivalents(beamVehicleType.passengerCarUnit)

    matsimVehicleType.setMaximumVelocity(beamVehicleType.maxVelocity.getOrElse(0.0))
    matsimVehicleType
  }

}
