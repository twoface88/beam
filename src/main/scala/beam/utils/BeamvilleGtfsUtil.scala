package beam.utils

import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.{Duration, LocalTime}

object BeamvilleGtfsUtil extends App{

  val formatter = DateTimeFormatter.ofPattern("kk:mm:ss")

  val routes = List("B1", "B2", "B3")
  val services = List("TR-2017-ALL-YEAR")
  val direction = List(("EAST", "0"), ("WEST", "1"))
  val routesWithStops = List(("B1", 1 to 5), ("B2", 6 to 10), ("B3", 11 to 15))

  val routesDirWithStop = for{
    (r, stopIds) <- routesWithStops
    (d, _) <- direction
  } yield {
    (s"$r-$d", stopIds)
  }

  def generateTrips(tripStops: Int = 5,
                    travels: Int = 4,
                    stopTime: Duration = Duration.ofMinutes(2),
                    travelTime: Duration = Duration.ofMinutes(1).plus(Duration.ofSeconds(30)),
                    startingTime: LocalTime = LocalTime.of(5, 58),
                    endingTime: LocalTime = LocalTime.of(20, 58)
                   ): List[List[String]] = {

    val totalTripTime: Duration =  stopTime.multipliedBy(tripStops).plus(travelTime.multipliedBy(travels))
    val adjustedTotalTripTime: Duration =  stopTime.multipliedBy(tripStops - 1).plus(travelTime.multipliedBy(travels))

    val totalMinutes = ChronoUnit.MINUTES.between(startingTime, endingTime)
    val totalTrips = totalMinutes / totalTripTime.toMinutes //64

    for{
      r <- routes
      s <- services
      tripId <- 1 to totalTrips.intValue()
      (d, did) <- direction
    } yield {
      List(s"$r-$d", r, s, s"$r-$d-$tripId", "", "", did, "", "", "0", "0")
    }
  }

  def generateTripStops(trips: List[List[String]],
                        startingTime: LocalTime = LocalTime.of(5, 58),
                        travelTime: Duration = Duration.ofMinutes(1).plus(Duration.ofSeconds(30)),
                        stopTime: Duration = Duration.ofMinutes(2),
                       ) = {
    val tripsGroupedByRoute = trips.groupBy(x => x.head)

    routesDirWithStop.map{ case (routeWithDir, stopIds) =>
      val tripsForRoute = tripsGroupedByRoute.get(routeWithDir).get
      var arrivalTime = startingTime
      var departureTime = arrivalTime.plus(stopTime)
      for{
        trip <- tripsForRoute
        (stopId, index) <- stopIds.zipWithIndex
      } yield{
        val r = List(
          trip(3),
          arrivalTime.format(formatter),
          departureTime.format(formatter),
          stopId.toString,
          (index + 1).toString,
          "0",
          "0"
        ).mkString(",")
        arrivalTime = departureTime.plus(travelTime)
        departureTime = arrivalTime.plus(stopTime)
        r
      }
    }.flatten
  }

  def generateGtfsData() = {

    val routes = List("B1", "B2", "B3")
    val services = List("TR-2017-ALL-YEAR")
    val direction = List(("EAST", "0"), ("WEST", "1"))
    val routesWithStops = List(("B1", 1 to 5), ("B2", 6 to 10), ("B3", 11 to 15))

    val routesDirWithStop = for{
      (r, stopIds) <- routesWithStops
      (d, _) <- direction
    } yield {
      (s"$r-$d", stopIds)
    }

    val tripStops = 5
    val stopTime = Duration.ofMinutes(2)
    val travels = 4
    val travelTime = Duration.ofMinutes(1).plus(Duration.ofSeconds(30))

    val totalTripTime: Duration =  stopTime.multipliedBy(tripStops).plus(travelTime.multipliedBy(travels))
    val adjustedTotalTripTime: Duration =  stopTime.multipliedBy(tripStops - 1).plus(travelTime.multipliedBy(travels))

    val startingTime = LocalTime.of(5, 58)
    val endingTime = LocalTime.of(20, 58)

    val totalMinutes = ChronoUnit.MINUTES.between(startingTime, endingTime)
    val totalTrips = totalMinutes / totalTripTime.toMinutes //64


    val trips = for{
      r <- routes
      s <- services
      tripId <- 1 to totalTrips.intValue()
      (d, did) <- direction
    } yield {
      List(s"$r-$d", r, s, s"$r-$d-$tripId", "", "", did, "", "", "0", "0")
    }

    val tripsGroupedByRoute = trips.groupBy(x => x.head)

    val stopTimes = routesDirWithStop.map{ case (routeWithDir, stopIds) =>
      val tripsForRoute = tripsGroupedByRoute.get(routeWithDir).get
      var arrivalTime = startingTime
      var departureTime = arrivalTime.plus(stopTime)
      for{
        trip <- tripsForRoute
        (stopId, index) <- stopIds.zipWithIndex
      } yield{
        val r = List(
          trip(3),
          arrivalTime.format(formatter),
          departureTime.format(formatter),
          stopId.toString,
          (index + 1).toString,
          "0",
          "0"
        ).mkString(",")
        arrivalTime = departureTime.plus(travelTime)
        departureTime = arrivalTime.plus(stopTime)
        r
      }
    }.flatten
  }

  generateGtfsData()

}
