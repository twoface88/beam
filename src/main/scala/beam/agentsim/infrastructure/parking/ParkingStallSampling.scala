package beam.agentsim.infrastructure.parking

import scala.util.Random

import beam.agentsim.infrastructure.taz.TAZ
import beam.router.BeamRouter.Location
import org.matsim.api.core.v01.Coord

/**
  * sampling methods for randomly generating stall locations from aggregate information
  */
object ParkingStallSampling {
  /**
    * generates stall locations per a sampling technique which induces noise as a function of stall attribute availability
    * @param rand random generator used to create stall locations
    * @param agent position of agent
    * @param taz position of TAZ centroid
    * @param availabilityRatio availability of the chosen stall type, as a ratio, i.e., in the range [0, 1]
    * @return a sampled location
    */
  def availabilityAwareSampling(rand: Random, agent: Location, taz: TAZ, availabilityRatio: Double): Location = {

    val xDistance: Double = taz.coord.getX - agent.getX
    val yDistance: Double = taz.coord.getY - agent.getY
    val tazCharacteristicRadius: Double = math.sqrt(taz.areaInSquareMeters) / 2

    // represent parking availability with a monotonically decreasing but not steep inverse log slope
    val availabilityFactor: Double = math.max(1.0, -0.25 * math.log(availabilityRatio))

    // finding a location between the agent and the TAZ centroid to sample from, scaled back by increased availability
    val (scaledXDistance, scaledYDistance) = (
      xDistance * availabilityFactor,
      yDistance * availabilityFactor
    )

    // random values, scaled to the problem size, but scaled back by increased availability
    val (sampleX, sampleY) = (
      rand.nextGaussian * tazCharacteristicRadius * availabilityFactor,
      rand.nextGaussian * tazCharacteristicRadius * availabilityFactor
    )

    // linear combination of current agent position, a scaled random variable, and a scaled sample centroid
    new Coord(
      agent.getX + sampleX + scaledXDistance,
      agent.getY + sampleY + scaledYDistance
    )
  }



  /**
    * samples a random location near a TAZ's centroid in order to create a stall in that TAZ.
    * previous dev's note: make these distributions more custom to the TAZ and stall type
    * @param rand random generator
    * @param center location we are sampling from
    *
    * @return a coordinate near that TAZ
    */
  def sampleLocationForStall(rand: Random, center: Location, radius: Double): Location = {
    val lambda = 0.01
    val deltaRadiusX = -math.log(1 - (1 - math.exp(-lambda * radius)) * rand.nextDouble()) / lambda
    val deltaRadiusY = -math.log(1 - (1 - math.exp(-lambda * radius)) * rand.nextDouble()) / lambda

    val x = center.getX + deltaRadiusX
    val y = center.getY + deltaRadiusY
    new Location(x, y)
  }
}
