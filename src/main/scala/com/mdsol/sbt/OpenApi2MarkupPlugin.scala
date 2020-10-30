package com.mdsol.sbt

import java.io.File
import java.nio.file.Paths

import _root_.io.github.swagger2markup.OpenAPI2MarkupConverter
import _root_.io.github.swagger2markup.config.builder.OpenAPI2MarkupConfigBuilder
import _root_.io.github.swagger2markup.utils.URIUtils
import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang3.StringUtils
import sbt.Keys._
import sbt.util.Logger
import sbt.{Def, _}

import scala.annotation.tailrec
import scala.collection.JavaConverters._

object OpenApi2MarkupPlugin extends AutoPlugin {

  object autoImport extends OpenApi2MarkupPluginKeys
  import autoImport._

  override lazy val projectSettings: Seq[Setting[_]] = Seq(
    processOpenApi := processOpenApiTask.value,
    openApi2MarkupProperties := Map.empty,
    openApi2MarkupInput := "",
    openApi2MarkupOutputDirectory := target.value / "generated-docs",
    openApi2MarkupOutputFile := None
  )

  private def processOpenApiTask: Def.Initialize[Task[Unit]] =
    Def.task {
      val logger = new PluginLogger(streams.value.log)
      val skp = skip.value
      val ref = thisProjectRef.value
      if (skp)
        logger.debug(s"Skipping OpenApi2Markup processing for ${ref.project}")
      else {
        logger.debug("processOpenApi task started")
        logger.debug(s"openApi2MarkupInput: ${openApi2MarkupInput.value}")
        logger.debug(s"openApi2MarkupOutputDirectory: ${openApi2MarkupOutputDirectory.value}")
        logger.debug(s"openApi2MarkupOutputFile: ${openApi2MarkupOutputFile.value}")
        val openApi2markupProperties = openApi2MarkupProperties.value
        openApi2markupProperties.foreach { config =>
          logger.debug(s"${config._1} : ${config._2}")
        }
        try {
          Thread
            .currentThread()
            .setContextClassLoader(
              this.getClass.getClassLoader
            )

          val openApi2MarkupConfig = new OpenAPI2MarkupConfigBuilder(openApi2markupProperties.asJava).build
          if (!openApi2MarkupInput.value.toLowerCase.startsWith("http")) {
            val inputFile = new File(openApi2MarkupInput.value)
            if (inputFile.exists) {
              if (inputFile.isDirectory) {
                logger.debug(s"Processing local directory: ${openApi2MarkupInput.value}")
                getOpenApiFiles(inputFile).foreach { openApiFile =>
                  openApiToMarkup(
                    openApi2MarkupInput.value,
                    openApi2MarkupOutputFile.value,
                    openApi2MarkupOutputDirectory.value,
                    OpenAPI2MarkupConverter.from(openApiFile.toURI).build,
                    inputIsLocalFolder = true,
                    logger
                  )
                }
              } else {
                logger.debug(s"Processing local file: ${openApi2MarkupInput.value}")
                openApiToMarkup(
                  openApi2MarkupInput.value,
                  openApi2MarkupOutputFile.value,
                  openApi2MarkupOutputDirectory.value,
                  OpenAPI2MarkupConverter.from(URIUtils.create(openApi2MarkupInput.value)).withConfig(openApi2MarkupConfig).build,
                  inputIsLocalFolder = false,
                  logger
                )
              }
            } else
              logger.error(s"Failed to execute goal 'processOpenApi': ${inputFile.getPath} doesn't exist")
          } else {
            logger.debug(s"Processing remote file: ${openApi2MarkupInput.value}")
            openApiToMarkup(
              openApi2MarkupInput.value,
              openApi2MarkupOutputFile.value,
              openApi2MarkupOutputDirectory.value,
              OpenAPI2MarkupConverter.from(URIUtils.create(openApi2MarkupInput.value)).withConfig(openApi2MarkupConfig).build,
              inputIsLocalFolder = false,
              logger
            )
          }
        } catch {
          case e: Exception =>
            throw new Exception(s"Failed to execute goal 'processOpenApi': \n\t${e.getMessage}", e)
        }
        logger.debug("processOpenApi goal finished")
      }
    }

  private def openApiToMarkup(
    inputFile: String,
    maybeOutputFile: Option[File],
    outputDirectory: File,
    converter: OpenAPI2MarkupConverter,
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
        } else
          outFile.toPath
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
    converter: OpenAPI2MarkupConverter,
    logger: PluginLogger
  ): File = {
    var outputDirAddendum = getInputDirStructurePath(inputFile, converter)
    if (hasMultipleOpenApiFiles(converter, logger))
      /*
       * If the folder the current OpenApi file resides in contains at least one other OpenApi file then the
       * output dir must have an extra subdir per file to avoid markdown files getting overwritten.
       */
      outputDirAddendum += File.separator + extractOpenApiFileNameWithoutExtension(converter)
    new File(outputDirectory, outputDirAddendum)
  }

  private def getInputDirStructurePath(inputFile: String, converter: OpenAPI2MarkupConverter): String = {
    /*
     * When the OpenApi input is a local folder (e.g. /Users/foo/) you'll want to group the generated output in the
     * configured output directory. The most obvious approach is to replicate the folder structure from the input
     * folder to the output folder. Example:
     * - openApi2MarkupInput is set to /Users/foo
     * - there's a single OpenApi file at /Users/foo/bar-service/v1/bar.yaml
     * - openApi2MarkupOutputDirectory is set to /tmp/asciidoc
     * -> markdown files from bar.yaml are generated to /tmp/asciidoc/bar-service/v1
     */
    val openApiFilePath = new File(converter.getContext.getSwaggerLocation).getAbsolutePath // /Users/foo/bar-service/v1/bar.yaml
    val openApiFileFolder = StringUtils.substringBeforeLast(openApiFilePath, File.separator) // /Users/foo/bar-service/v1
    StringUtils.remove(openApiFileFolder, getOpenApiInputAbsolutePath(inputFile)) // /bar-service/v1
  }

  private def hasMultipleOpenApiFiles(converter: OpenAPI2MarkupConverter, logger: PluginLogger): Boolean =
    getOpenApiFiles(new File(converter.getContext.getSwaggerLocation).getParentFile, recursive = false).nonEmpty

  private def extractOpenApiFileNameWithoutExtension(converter: OpenAPI2MarkupConverter): String =
    FilenameUtils.removeExtension(new File(converter.getContext.getSwaggerLocation).getName)

  def getOpenApiFiles(location: File, recursive: Boolean = true): Seq[File] = {
    @tailrec
    def go(toCheck: List[File], results: List[File]): Seq[File] =
      toCheck match {
        case head :: tail =>
          val allFiles: List[File] = if (head.isFile) List(head) else head.listFiles.toList
          val openApiFiles = allFiles.filter(_.isFile).filter(f => f.getName.endsWith("yaml") || f.getName.endsWith("yml") || f.getName.endsWith("json"))
          val childDirs = allFiles.filter(_.isDirectory)
          val updated = if (allFiles.length == childDirs.length) results else openApiFiles ++ results
          if (recursive)
            go(tail ++ childDirs, updated)
          else
            openApiFiles
        case _ => results
      }
    go(location :: Nil, Nil)
  }

  /*
   * The 'openApi2MarkupInput' provided by the user can be anything; it's just a string. Hence, it could by Unix-style,
   * Windows-style or even a mix thereof. This methods turns the input into a File and returns its absolute path. It
   * will be platform dependent as far as file separators go but at least the separators will be consistent.
   */
  private def getOpenApiInputAbsolutePath(inputFile: String): String = new File(inputFile).absolutePath

  private class PluginLogger(logger: Logger) {
    def debug(msg: String): Unit = logger.debug(s"[sbt-openApi2markup] $msg")

    def info(msg: String): Unit = logger.info(s"[sbt-openApi2markup] $msg")

    def error(msg: String): Unit = logger.error(s"[sbt-openApi2markup] $msg")

    def warn(msg: String): Unit = logger.warn(s"[sbt-openApi2markup] $msg")
  }

}

trait OpenApi2MarkupPluginKeys {
  val processOpenApi: TaskKey[Unit] = taskKey[Unit]("Convert OpenApi files to target files")

  val openApi2MarkupProperties: SettingKey[Map[String, String]] = settingKey[Map[String, String]]("OpenApi2Markup configuration properties")
  val openApi2MarkupInput: SettingKey[String] = settingKey[String]("OpenApi input URI")
  val openApi2MarkupOutputDirectory: SettingKey[File] = settingKey[File]("Default directory target directory to contain converted files")
  val openApi2MarkupOutputFile: SettingKey[Option[File]] = settingKey[Option[File]]("Used to override the name of the generated output file")
}
