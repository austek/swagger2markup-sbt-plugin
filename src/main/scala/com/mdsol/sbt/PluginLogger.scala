package com.mdsol.sbt

import sbt.util.Logger

class PluginLogger(logger: Logger) {
  def debug(msg: String): Unit = logger.debug(s"[sbt-asciidoctor] $msg")

  def info(msg: String): Unit = logger.info(s"[sbt-asciidoctor] $msg")

  def error(msg: String): Unit = logger.error(s"[sbt-asciidoctor] $msg")

  def warn(msg: String): Unit = logger.warn(s"[sbt-asciidoctor] $msg")
}
