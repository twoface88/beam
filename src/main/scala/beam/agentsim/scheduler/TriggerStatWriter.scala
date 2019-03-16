package beam.agentsim.scheduler
import java.io.Writer

import akka.Done
import akka.actor.{Actor, ActorLogging}
import beam.agentsim.scheduler.BeamAgentScheduler.ScheduledTrigger
import beam.agentsim.scheduler.TriggerStatWriter.Info
import beam.analysis.via.CSVWriter
import beam.utils.StuckFinder

object TriggerStatWriter {
  case class Info(
    completionNoticeId: Int,
    trigger: ScheduledTrigger,
    duration: Long,
    awaitingResponseSize: Int,
    triggerQueueSize: Int,
    prevCompletionNoticeTs: Long,
    currentCompletionNoticeTs: Long,
    diffCompletionNoticeTs: Long
  )
}

class TriggerStatWriter(val iterNumber: Int, val outputFolder: String) extends Actor with ActorLogging {
  val csvWriter: CSVWriter = new CSVWriter(s"$outputFolder/it_${iterNumber}_trigger_stats.csv.gz")
  val bufferedWriter = csvWriter.getBufferedWriter

  writeHeader(bufferedWriter)

  var totalWrittenRows: Int = 0

  log.info("TriggerStatWriter is ready")

  override def receive: Receive = {
    case info: Info =>
      writeTriggerStat(info, bufferedWriter)
      totalWrittenRows += 1
      if (totalWrittenRows % 10000 == 0) {
        bufferedWriter.flush()
      }
    case Done =>
      log.info(s"Received Done!. totalWrittenRows: $totalWrittenRows")
      context.stop(self)
  }

  def writeTriggerStat(info: Info, bufferedWriter: Writer): Unit = {

    bufferedWriter.append(info.completionNoticeId.toString) // id
    bufferedWriter.append(",")

    val trigger = info.trigger
    bufferedWriter.append(trigger.agent.path.toSerializationFormat) // actor_name
    bufferedWriter.append(",")

    bufferedWriter.append(StuckFinder.getActorType(trigger.agent)) // actor_type
    bufferedWriter.append(",")

    bufferedWriter.append(trigger.triggerWithId.trigger.getClass.getSimpleName) // trigger_type
    bufferedWriter.append(",")

    bufferedWriter.append(trigger.triggerWithId.trigger.tick.toString) // tick
    bufferedWriter.append(",")

    bufferedWriter.append(info.duration.toString) // duration
    bufferedWriter.append(",")

    bufferedWriter.append(info.awaitingResponseSize.toString) // awaiting_response_size
    bufferedWriter.append(",")

    bufferedWriter.append(info.triggerQueueSize.toString) // trigger_queue_size
    bufferedWriter.append(",")

    bufferedWriter.append(info.prevCompletionNoticeTs.toString) // prev_completion_notice_ts
    bufferedWriter.append(",")

    bufferedWriter.append(info.currentCompletionNoticeTs.toString) // current_completion_notice_ts
    bufferedWriter.append(",")

    bufferedWriter.append(info.diffCompletionNoticeTs.toString) // diff_completion_notice_ts

    bufferedWriter.append(System.lineSeparator())

    ()
  }

  def writeHeader(writer: Writer): Unit = {
    writer.append("id")
    writer.append(",")

    writer.append("actor_name")
    writer.append(",")

    writer.append("actor_type")
    writer.append(",")

    writer.append("trigger_type")
    writer.append(",")

    writer.append("tick")
    writer.append(",")

    writer.append("duration")
    writer.append(",")

    writer.append("awaiting_response_size")
    writer.append(",")

    writer.append("trigger_queue_size")
    writer.append(",")

    writer.append("prev_completion_notice_ts")
    writer.append(",")

    writer.append("current_completion_notice_ts")
    writer.append(",")

    writer.append("diff_completion_notice_ts")

    writer.append(System.lineSeparator())
    ()
  }

  override def aroundPostStop(): Unit = {
    log.info(s"aroundPostStop. totalWrittenRows: ${totalWrittenRows}")
    csvWriter.getBufferedWriter.flush()
    csvWriter.closeFile()
    super.aroundPostStop()
  }
}
