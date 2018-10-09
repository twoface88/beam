package beam.calibration.utils

object ParamNameShortener extends App {

  var abbrevToParamsMap: Map[String, String] = Map()

  def shortenName(paramName: String) = {
    val paramParts: Array[String] = paramName.split("""\.""")

    var paramParts2: Array[String] = Array()
    for(i <- 0 until paramParts.length - 1){

      val pp = paramParts(i)
      paramParts2 = paramParts2 :+ pp.take(1)
    }

    paramParts2 = paramParts2 :+ paramParts(paramParts.length - 1)
    val shortName = paramParts2.mkString(".")

    abbrevToParamsMap = abbrevToParamsMap + (shortName -> paramName)
  }

  def expandName(shortName: String): String ={

    abbrevToParamsMap(shortName)
  }
}
