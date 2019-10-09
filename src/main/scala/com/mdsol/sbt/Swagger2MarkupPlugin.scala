package com.mdsol.sbt

import java.io.File
import java.nio.file.Paths

import _root_.io.github.swagger2markup.Swagger2MarkupConverter
import _root_.io.github.swagger2markup.builder.Swagger2MarkupConfigBuilder
import _root_.io.github.swagger2markup.utils.URIUtils
import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang3.StringUtils
import sbt.Keys._
import sbt.util.Logger
import sbt.{Def, _}

import scala.annotation.tailrec
import scala.collection.JavaConverters._

object Swagger2MarkupPlugin extends AutoPlugin {

  object autoImport extends Swagger2MarkupPluginKeys
  import autoImport._

  override lazy val projectSettings: Seq[Setting[_]] = Seq(
    processSwagger := processSwaggerTask.value,
    swagger2MarkupProperties := Map.empty,
    swagger2MarkupInput := "",
    swagger2MarkupOutputDirectory := target.value / "generated-docs",
    swagger2MarkupOutputFile := None
  )

  private def processSwaggerTask: Def.Initialize[Task[Unit]] = Def.task {
    val logger = new PluginLogger(streams.value.log)
    val skp = skip.value
    val ref = thisProjectRef.value
    if (skp) {
      logger.debug(s"Skipping Swagger2Markup processing for ${ref.project}")
    } else if (!sourceDirectory.value.exists) {
      logger.info(s"sourceDirectory ${sourceDirectory.value.getPath} does not exist. Skip processing")
    } else {
      logger.debug("processSwagger task started")
      logger.debug(s"swagger2MarkupInput: ${swagger2MarkupInput.value}")
      logger.debug(s"swagger2MarkupOutputDirectory: ${swagger2MarkupOutputDirectory.value}")
      logger.debug(s"swagger2MarkupOutputFile: ${swagger2MarkupOutputFile.value}")
      val swagger2markupProperties = swagger2MarkupProperties.value
      swagger2markupProperties.foreach { config =>
        logger.debug(s"${config._1} : ${config._2}")
      }
      try {
        Thread
          .currentThread()
          .setContextClassLoader(
            this.getClass.getClassLoader
          )

        val swagger2MarkupConfig = new Swagger2MarkupConfigBuilder(swagger2markupProperties.asJava).build
        if (!swagger2MarkupInput.value.toLowerCase.startsWith("http")) {
          val inputFile = new File(swagger2MarkupInput.value)
          if (inputFile.exists) {
            if (inputFile.isDirectory) {
              logger.debug(s"Processing local directory: ${swagger2MarkupInput.value}")
              getSwaggerFiles(inputFile).foreach { swaggerFile =>
                swaggerToMarkup(
                  swagger2MarkupInput.value,
                  swagger2MarkupOutputFile.value,
                  swagger2MarkupOutputDirectory.value,
                  Swagger2MarkupConverter.from(swaggerFile.toURI).build,
                  inputIsLocalFolder = true,
                  logger
                )
              }
            } else {
              logger.debug(s"Processing local file: ${swagger2MarkupInput.value}")
              swaggerToMarkup(
                swagger2MarkupInput.value,
                swagger2MarkupOutputFile.value,
                swagger2MarkupOutputDirectory.value,
                Swagger2MarkupConverter.from(URIUtils.create(swagger2MarkupInput.value)).withConfig(swagger2MarkupConfig).build,
                inputIsLocalFolder = false,
                logger
              )
            }
          } else {
            logger.error(s"Failed to execute goal 'processSwagger': ${inputFile.getPath} doesn't exist")
          }
        } else {
          logger.debug(s"Processing remote file: ${swagger2MarkupInput.value}")
          swaggerToMarkup(
            swagger2MarkupInput.value,
            swagger2MarkupOutputFile.value,
            swagger2MarkupOutputDirectory.value,
            Swagger2MarkupConverter.from(URIUtils.create(swagger2MarkupInput.value)).withConfig(swagger2MarkupConfig).build,
            inputIsLocalFolder = false,
            logger
          )
        }
      } catch {
        case e: Exception =>
          throw new Exception(s"Failed to execute goal 'processSwagger': \n\t${e.getMessage}", e)
      }
      logger.debug("processSwagger goal finished")
    }
  }

  private def swaggerToMarkup(
    inputFile: String,
    maybeOutputFile: Option[File],
    outputDirectory: File,
    converter: Swagger2MarkupConverter,
    inputIsLocalFolder: Boolean,
    logger: PluginLogger
  ): Unit = {
    maybeOutputFile match {
      case Some(outFile) =>
        /*
         * If user has specified input folder with multiple files to convert,
         * and has specified a single output file, then route all conversions
         * into one file under each 'new' sub-directory, which corresponds to
         * each input file.
         * Otherwise, specifying the output file with an input DIRECTORY means
         * last file converted wins.
         */
        val useFile = if (inputIsLocalFolder) {
          val effectiveOutputDir = getEffectiveOutputDirWhenInputIsAFolder(inputFile, outputDirectory, converter, logger)
          logger.debug(s"Effective Output Directory: $outputDirectory")
          converter.getContext.setOutputPath(effectiveOutputDir.toPath)
          Paths.get(effectiveOutputDir.getPath, outFile.toPath.getFileName.toString)
        } else {
          outFile.toPath
        }
        logger.info(s"Converting input to one file: $useFile")
        converter.toFile(useFile)
      case None =>
        var effectiveOutputDir = outputDirectory
        if (inputIsLocalFolder) effectiveOutputDir = getEffectiveOutputDirWhenInputIsAFolder(inputFile, outputDirectory, converter, logger)
        logger.info(s"Converting input to multiple files in folder: '$effectiveOutputDir'")
        converter.toFolder(effectiveOutputDir.toPath)
    }
  }

  private def getEffectiveOutputDirWhenInputIsAFolder(
    inputFile: String,
    outputDirectory: File,
    converter: Swagger2MarkupConverter,
    logger: PluginLogger
  ): File = {
    var outputDirAddendum = getInputDirStructurePath(inputFile, converter)
    if (hasMultipleSwaggerFiles(converter, logger)) {
      /*
       * If the folder the current Swagger file resides in contains at least one other Swagger file then the
       * output dir must have an extra subdir per file to avoid markdown files getting overwritten.
       */
      outputDirAddendum += File.separator + extractSwaggerFileNameWithoutExtension(converter)
    }
    new File(outputDirectory, outputDirAddendum)
  }

  private def getInputDirStructurePath(inputFile: String, converter: Swagger2MarkupConverter): String = {
    /*
     * When the Swagger input is a local folder (e.g. /Users/foo/) you'll want to group the generated output in the
     * configured output directory. The most obvious approach is to replicate the folder structure from the input
     * folder to the output folder. Example:
     * - swagger2MarkupInput is set to /Users/foo
     * - there's a single Swagger file at /Users/foo/bar-service/v1/bar.yaml
     * - swagger2MarkupOutputDirectory is set to /tmp/asciidoc
     * -> markdown files from bar.yaml are generated to /tmp/asciidoc/bar-service/v1
     */
    val swaggerFilePath = new File(converter.getContext.getSwaggerLocation).getAbsolutePath // /Users/foo/bar-service/v1/bar.yaml
    val swaggerFileFolder = StringUtils.substringBeforeLast(swaggerFilePath, File.separator) // /Users/foo/bar-service/v1
    StringUtils.remove(swaggerFileFolder, getSwaggerInputAbsolutePath(inputFile)) // /bar-service/v1
  }

  private def hasMultipleSwaggerFiles(converter: Swagger2MarkupConverter, logger: PluginLogger): Boolean =
    getSwaggerFiles(new File(converter.getContext.getSwaggerLocation).getParentFile, recursive = false).nonEmpty

  private def extractSwaggerFileNameWithoutExtension(converter: Swagger2MarkupConverter): String =
    FilenameUtils.removeExtension(new File(converter.getContext.getSwaggerLocation).getName)

  def getSwaggerFiles(location: File, recursive: Boolean = true): Seq[File] = {
    @tailrec
    def go(toCheck: List[File], results: List[File]): Seq[File] = toCheck match {
      case head :: tail =>
        val allFiles: List[File] = if (head.isFile) List(head) else head.listFiles.toList
        val swaggerFiles = allFiles.filter(_.isFile).filter(f => f.getName.endsWith("yaml") || f.getName.endsWith("yml") || f.getName.endsWith("json"))
        val childDirs = allFiles.filter(_.isDirectory)
        val updated = if (allFiles.length == childDirs.length) results else swaggerFiles ++ results
        if (recursive) {
          go(tail ++ childDirs, updated)
        } else {
          swaggerFiles
        }
      case _ => results
    }
    go(location :: Nil, Nil)
  }

  /*
   * The 'swagger2MarkupInput' provided by the user can be anything; it's just a string. Hence, it could by Unix-style,
   * Windows-style or even a mix thereof. This methods turns the input into a File and returns its absolute path. It
   * will be platform dependent as far as file separators go but at least the separators will be consistent.
   */
  private def getSwaggerInputAbsolutePath(inputFile: String): String = new File(inputFile).absolutePath

  private class PluginLogger(logger: Logger) {
    def debug(msg: String): Unit = logger.debug(s"[sbt-swagger2markup] $msg")

    def info(msg: String): Unit = logger.info(s"[sbt-swagger2markup] $msg")

    def error(msg: String): Unit = logger.error(s"[sbt-swagger2markup] $msg")

    def warn(msg: String): Unit = logger.warn(s"[sbt-swagger2markup] $msg")
  }

}

trait Swagger2MarkupPluginKeys {
  val processSwagger: TaskKey[Unit] = taskKey[Unit]("Convert OpenApi files to target files")

  val swagger2MarkupProperties: SettingKey[Map[String, String]] = settingKey[Map[String, String]]("Swagger2Markup configuration properties")
  val swagger2MarkupInput: SettingKey[String] = settingKey[String]("Swagger input URI")
  val swagger2MarkupOutputDirectory: SettingKey[File] = settingKey[File]("Default directory target directory to contain converted files")
  val swagger2MarkupOutputFile: SettingKey[Option[File]] = settingKey[Option[File]]("Used to override the name of the generated output file")
}
