package de.daxten.utils

import org.scalafmt.Scalafmt
import better.files._

object ScalaFmtHelper {
  private val style = {
    val file = file".scalafmt.conf"

    if (file.exists)
      org.scalafmt.config.Config
        .fromHoconString(scala.io.Source.fromFile(file.toJava).mkString)
        .getOrElse(org.scalafmt.config.ScalafmtConfig.default120)
    else
      org.scalafmt.config.ScalafmtConfig.default120
  }
  def formatCode(code: String): String = Scalafmt.format(code, style).get
}
