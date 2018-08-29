package beam.integration

import java.io.File

import org.scalatest.Ignore

@Ignore
trait ReadEvents {

  def getListTagsFromFile(
    file: File,
    mkeyValue: Option[(String, String)] = None,
    tagToReturn: String,
    eventType: Option[String] = None,
    tagTwoToReturn: Option[String] = None
  ): Seq[String]

  def getListTagsFrom(
    filePath: String,
    mkeyValue: Option[(String, String)] = None,
    tagToReturn: String,
    eventType: Option[String] = None
  ): Seq[String]

  def getListTwoTagsFromFile(
    file: File,
    mkeyValue: Option[(String, String)] = None,
    tagToReturn: String,
    eventType: Option[String] = None,
    tagTwoToReturn: Option[String] = None
  ): Seq[(String, String)]

  def getLinesFrom(file: File): String
}
