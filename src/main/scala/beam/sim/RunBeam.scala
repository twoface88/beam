package beam.sim

object RunBeam extends BeamHelper {

  def main(args: Array[String]): Unit = {

    print(beamAsciiArt)

    runBeamUsing(args)
    logger.info("Exiting BEAM")
    System.exit(0)
  }

}
