package com.mdsol.sbt

import java.io.File
import java.nio.file.Paths

import _root_.io.github.swagger2markup.Swagger2MarkupConverter
import _root_.io.github.swagger2markup.builder.Swagger2MarkupConfigBuilder
import _root_.io.github.swagger2markup.utils.URIUtils
import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang3.StringUtils
import sbt.Keys._
import sbt.librarymanagement.Configuration
import sbt.librarymanagement.Configurations.Runtime
import sbt.{Def, _}

import scala.annotation.tailrec
import scala.collection.JavaConverters._

object Swagger2MarkupPlugin extends AutoPlugin {

  object autoImport extends Swagger2MarkupPluginKeys
  import autoImport._

  lazy val asciiDoctorSettings: Seq[Setting[_]] = Seq(
    processSwagger := processAsciiDocTask.value,
    properties := Map.empty,
    swaggerInput := "",
    outputDirectory := target.value / "generated-docs",
    outputFile := None
  )
  override lazy val projectSettings: Seq[Setting[_]] = inConfig(Swagger2Markup)(asciiDoctorSettings)

  private def processAsciiDocTask: Def.Initialize[Task[Swagger2MarkupResult]] = Def.task[Swagger2MarkupResult] {
    val logger = new PluginLogger(streams.value.log)
    val skp = (skip in publish).value
    val ref = thisProjectRef.value
    if (skp) {
      logger.debug(s"Skipping AsciiDoc processing for ${ref.project}")
      Skipped
    }
    if (!sourceDirectory.value.exists) {
      logger.info(s"sourceDirectory ${sourceDirectory.value.getPath} does not exist. Skip processing")
      Skipped
    }

    logger.debug("convertSwagger2markup goal started")
    logger.debug("swaggerInput: " + swaggerInput.value)
    logger.debug("outputDir: " + outputDirectory.value)
    logger.debug("outputFile: " + outputFile.value)
    val swagger2markupProperties = properties.value.map{case (k, v) => s"swagger2markup.$k" -> v}
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
      logger.info(s"SSSSSSSSSSS: " + swagger2MarkupConfig.isSeparatedDefinitionsEnabled)
      logger.info(s"OOOOOOOOOOO: " + swagger2MarkupConfig.isSeparatedOperationsEnabled)
      logger.info(s"MMMMMMMMMMM: " + swagger2MarkupConfig.getMarkupLanguage)
      if (isLocalFolder(swaggerInput.value)) {
        getSwaggerFiles(new File(swaggerInput.value)).foreach { swaggerFile =>
          swaggerToMarkup(
            swaggerInput.value,
            outputFile.value,
            outputDirectory.value,
            Swagger2MarkupConverter.from(swaggerFile.toURI).build,
            inputIsLocalFolder = true,
            logger
          )
        }
      } else {
        swaggerToMarkup(
          swaggerInput.value,
          outputFile.value,
          outputDirectory.value,
          Swagger2MarkupConverter.from(URIUtils.create(swaggerInput.value)).withConfig(swagger2MarkupConfig).build,
          inputIsLocalFolder = true,
          logger
        )
      }
    } catch {
      case e: Exception =>
        throw new Exception("Failed to execute goal 'processSwagger'\n\t" + e.getMessage, e)
    }
    logger.debug("processSwagger goal finished")

    Success
  }

  private def isLocalFolder(swaggerInput: String): Boolean = !swaggerInput.toLowerCase.startsWith("http") && new File(swaggerInput).isDirectory

  private def swaggerToMarkup(inputFile: String,
                              outputFile: Option[File],
                              outputDirectory: File,
                              converter: Swagger2MarkupConverter,
                              inputIsLocalFolder: Boolean,
                              logger: PluginLogger): Unit = {
    if (outputFile.isDefined) {
      var useFile = outputFile.get.toPath
      /*
       * If user has specified input folder with multiple files to convert,
       * and has specified a single output file, then route all conversions
       * into one file under each 'new' sub-directory, which corresponds to
       * each input file.
       * Otherwise, specifying the output file with an input DIRECTORY means
       * last file converted wins.
       */
      if (inputIsLocalFolder) if (outputDirectory != null) {
        var effectiveOutputDir = outputDirectory
        effectiveOutputDir = getEffectiveOutputDirWhenInputIsAFolder(inputFile, outputDirectory, converter)
        converter.getContext.setOutputPath(effectiveOutputDir.toPath)
        useFile = Paths.get(effectiveOutputDir.getPath, useFile.getFileName.toString)
      }
      logger.info("Converting input to one file: " + useFile)
      converter.toFile(useFile)
    } else if (outputDirectory != null) {
      var effectiveOutputDir = outputDirectory
      if (inputIsLocalFolder) effectiveOutputDir = getEffectiveOutputDirWhenInputIsAFolder(inputFile, outputDirectory, converter)
      logger.info("Converting input to multiple files in folder: '" + effectiveOutputDir + "'")
      converter.toFolder(effectiveOutputDir.toPath)
    } else throw new IllegalArgumentException("Either outputFile or outputDir parameter must be used")
  }

  private def getEffectiveOutputDirWhenInputIsAFolder(inputFile: String, outputDirectory: File, converter: Swagger2MarkupConverter) = {
    var outputDirAddendum = getInputDirStructurePath(inputFile, converter)
    if (hasMultipleSwaggerFiles(converter)) {
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
     * - swaggerInput is set to /Users/foo
     * - there's a single Swagger file at /Users/foo/bar-service/v1/bar.yaml
     * - outputDir is set to /tmp/asciidoc
     * -> markdown files from bar.yaml are generated to /tmp/asciidoc/bar-service/v1
     */
    val swaggerFilePath = new File(converter.getContext.getSwaggerLocation).getAbsolutePath // /Users/foo/bar-service/v1/bar.yaml
    val swaggerFileFolder = StringUtils.substringBeforeLast(swaggerFilePath, File.separator) // /Users/foo/bar-service/v1
    StringUtils.remove(swaggerFileFolder, getSwaggerInputAbsolutePath(inputFile)) // /bar-service/v1

  }

  private def hasMultipleSwaggerFiles(converter: Swagger2MarkupConverter): Boolean = {
    getSwaggerFiles(new File(converter.getContext.getSwaggerLocation).getParentFile, recursive = false).nonEmpty
  }

  private def extractSwaggerFileNameWithoutExtension(converter: Swagger2MarkupConverter): String =
    FilenameUtils.removeExtension(new File(converter.getContext.getSwaggerLocation).getName)

  def getSwaggerFiles(location: File, recursive: Boolean = true): Seq[File] = {
    @tailrec
    def go(toCheck: List[File], results: List[File]): Seq[File] = toCheck match {
      case head :: tail =>
        val allFiles = head.listFiles.toList
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
   * The 'swaggerInput' provided by the user can be anything; it's just a string. Hence, it could by Unix-style,
   * Windows-style or even a mix thereof. This methods turns the input into a File and returns its absolute path. It
   * will be platform dependent as far as file separators go but at least the separators will be consistent.
   */
  private def getSwaggerInputAbsolutePath(inputFile: String): String = new File(inputFile).absolutePath

}

trait Swagger2MarkupPluginKeys {
  val Swagger2Markup: Configuration = Configuration.of("Swagger2Markup", "swagger2markup") extend Runtime
  val processSwagger: TaskKey[Unit] = taskKey[Unit]("Convert OpenApi files to target files")

  val properties: SettingKey[Map[String, String]] = settingKey[Map[String, String]]("Swagger2Markup configuration properties")
  val swaggerInput: SettingKey[String] = settingKey[String]("Swagger input URI")
  val outputDirectory: SettingKey[File] = settingKey[File]("Default directory target directory to contain converted files")
  val outputFile: SettingKey[Option[File]] = settingKey[Option[File]]("Used to override the name of the generated output file")
}
