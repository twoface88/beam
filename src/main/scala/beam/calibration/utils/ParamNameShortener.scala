package beam.calibration.utils
import scala.util.control.Breaks._

object ParamNameShortener extends App {

  var abbrevToParamsMap: Map[String, String] = Map()

  def shortenName(paramName: String) = {

    if(paramName.size > 100) {
      val paramParts: Array[String] = paramName.split("""\.""")

      var finalString: String = ""
      var firstPart: String = ""
      var restOfString: String = paramName

      breakable {
        for (i <- 0 until paramParts.length - 1) {

          val pp = paramParts(i)
          val firstChar = pp.take(1)

          if(firstPart.size > 0){
            firstPart = firstPart + "." + firstChar
          }else{
            firstPart = firstChar
          }

          restOfString = restOfString.substring(restOfString.indexOf(".") + 1)

          finalString = firstPart + "." + restOfString

          if (finalString.size <= 100) break

        }
      }

      abbrevToParamsMap = abbrevToParamsMap + (finalString -> paramName)
    }else{
      abbrevToParamsMap = abbrevToParamsMap + (paramName -> paramName)
    }
  }

  def expandName(shortName: String): String ={

    abbrevToParamsMap(shortName)
  }

  def logParamsMap() = {
    abbrevToParamsMap.foreach{
      case(k, v) => println(k + ", " + v)
    }
  }
}
