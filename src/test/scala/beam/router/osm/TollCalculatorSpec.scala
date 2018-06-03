package beam.router.osm

import java.nio.file.{Path, Paths}

import org.scalatest.{ParallelTestExecution, WordSpecLike}

import scala.language.postfixOps

//Tolls on osm ids: 79,87,109,147,155,163,1003,1005
class TollCalculatorSpec extends WordSpecLike with ParallelTestExecution {
  "Using beamville as input" when {
    val beamvillePath: Path = Paths.get("test", "input", "beamville", "r5")
    val beamvilleTollCalc = new TollCalculator(beamvillePath.toString)
    "calculate toll for a single trunk road, it" should {
      "return value $1." in {
        assert(beamvilleTollCalc.calcToll(Vector(109)) == 1.0)
      }
    }

    "calculate toll for a three trunk road, it" should {
      "return value $3." in {
        assert(beamvilleTollCalc.calcToll(Vector(109, 155, 163)) == 3.0)
      }
    }

    "calculate toll for a highway, it" should {
      "return value $6." in {
        assert(beamvilleTollCalc.calcToll(Vector(1003)) == 6.0)
      }
    }

    "calculate toll for a highway and a trunk road, it" should {
      "return value $7." in {
        assert(beamvilleTollCalc.calcToll(Vector(1003, 79)) == 7.0)
      }
    }
  }
}
