package beam.agentsim.infrastructure.charging

import beam.agentsim.infrastructure.charging.ElectricCurrentType.{AC, DC}

import scala.util.matching.Regex
import scala.util.{Failure, Success, Try}

sealed trait ChargingPointType

case object ChargingPointType {

  // some standard implementations of currently existing charging points
  case object HouseholdSocket extends ChargingPointType

  case object BlueHouseholdSocket extends ChargingPointType

  case object Cee16ASocket extends ChargingPointType

  case object Cee32ASocket extends ChargingPointType

  case object Cee63ASocket extends ChargingPointType

  case object ChargingStationType1 extends ChargingPointType

  case object ChargingStationType2 extends ChargingPointType

  case object ChargingStationCcsComboType1 extends ChargingPointType

  case object ChargingStationCcsComboType2 extends ChargingPointType

  case object TeslaSuperCharger extends ChargingPointType

  // provide custom charging points
  case class CustomChargingPoint(id: String, installedCapacity: Double, electricCurrentType: ElectricCurrentType) extends ChargingPointType

  case object CustomChargingPoint {

    def apply(id: String, installedCapacity: String, electricCurrentType: String): CustomChargingPoint = {
      Try {
        installedCapacity.toDouble
      } match {
        case Failure(_) =>
          throw new IllegalArgumentException(s"provided 'installed capacity' $installedCapacity is invalid.")
        case Success(installedCapacityDouble) =>
          CustomChargingPoint(id, installedCapacityDouble, ElectricCurrentType(electricCurrentType))
      }
    }

  }

  private[ChargingPointType] val CustomChargingPointRegex: Regex = "(\\w+)\\((\\d+),(\\w+)\\)".r

  // matches either the standard ones or a custom one
  def apply(s: String): Option[ChargingPointType] = {
    s.trim match {
      case CustomChargingPointRegex(
      id,
      installedCapacity,
      currentType
      ) => {
        Some(CustomChargingPoint(id, installedCapacity, currentType))
      }
      case "HouseholdSocket" => Some(HouseholdSocket)
      case "BlueHouseholdSocket" => Some(BlueHouseholdSocket)
      case "Cee16ASocket" => Some(Cee16ASocket)
      case "Cee32ASocket" => Some(Cee32ASocket)
      case "Cee63ASocket" => Some(Cee63ASocket)
      case "ChargingStationType1" => Some(ChargingStationType1)
      case "ChargingStationType2" => Some(ChargingStationType2)
      case "ChargingStationCcsComboType1" => Some(ChargingStationCcsComboType1)
      case "ChargingStationCcsComboType2" => Some(ChargingStationCcsComboType2)
      case "TeslaSuperCharger" => Some(TeslaSuperCharger)
      case "Level1" => Some(HouseholdSocket)
      case "Level2" => Some(ChargingStationType1)
      case "DCFast" => Some(ChargingStationCcsComboType2)
      case "UltraFast" => Some(CustomChargingPoint(s.trim, "250", "dc"))
      case "NoCharger" => None
      case "" => None
      case _ => throw new IllegalArgumentException("invalid argument for chargingPoint: " + s.trim)
    }
  }


  // matches either the standard ones or a custom one
  def getChargingPointInstalledPowerInKw(chargingPoint: ChargingPointType): Double = {
    chargingPoint match {
      case HouseholdSocket => 2.3
      case BlueHouseholdSocket => 3.6
      case Cee16ASocket => 11
      case Cee32ASocket => 22
      case Cee63ASocket => 43
      case ChargingStationType1 => 7.2
      case ChargingStationType2 => 43
      case ChargingStationCcsComboType1 => 11
      case ChargingStationCcsComboType2 => 50
      case TeslaSuperCharger => 135
      case CustomChargingPoint(_, v, _) => v
      case _ => throw new IllegalArgumentException("invalid argument")
    }
  }

  def getChargingPointCurrent(chargingPoint: ChargingPointType): ElectricCurrentType = {
    chargingPoint match {
      case HouseholdSocket => AC
      case BlueHouseholdSocket => AC
      case Cee16ASocket => AC
      case Cee32ASocket => AC
      case Cee63ASocket => AC
      case ChargingStationType1 => AC
      case ChargingStationType2 => AC
      case ChargingStationCcsComboType1 => DC
      case ChargingStationCcsComboType2 => DC
      case TeslaSuperCharger => DC
      case CustomChargingPoint(_, _, c) => c
      case _ => throw new IllegalArgumentException("invalid argument")
    }
  }

  def calculateChargingSessionLengthAndEnergyInJoule(
                                                      chargingPoint: ChargingPointType,
                                                      currentEnergyLevelInJoule: Double,
                                                      batteryCapacityInJoule: Double,
                                                      vehicleAcChargingLimitsInWatts: Double,
                                                      vehicleDcChargingLimitsInWatts: Double,
                                                      sessionDurationLimit: Option[Long]
                                                    ): (Long, Double) = {
    val chargingLimits = ChargingPointType.getChargingPointCurrent(chargingPoint) match {
      case AC => (vehicleAcChargingLimitsInWatts / 1000.0, batteryCapacityInJoule)
      case DC =>
        (vehicleDcChargingLimitsInWatts / 1000.0, batteryCapacityInJoule * 0.8) // DC limits charging to 0.8 * battery capacity
    }
    val sessionLengthLimiter = sessionDurationLimit.getOrElse(Long.MaxValue)
    val sessionLength = Math.min(
      sessionLengthLimiter,
      Math.round(
        (chargingLimits._2 - currentEnergyLevelInJoule) / 3.6e6 / Math
          .min(chargingLimits._1, ChargingPointType.getChargingPointInstalledPowerInKw(chargingPoint)) * 3600.0
      )
    )
    val sessionEnergyInJoules = sessionLength.toDouble / 3600.0 * Math.min(
      chargingLimits._1,
      ChargingPointType.getChargingPointInstalledPowerInKw(chargingPoint)
    ) * 3.6e6
    (sessionLength, sessionEnergyInJoules)
  }

}