lazy val root = (project in file("."))
  .enablePlugins(Swagger2MarkupPlugin)
  .settings(
    Swagger2Markup / swaggerInput := "src/main/swagger/swagger.json",
    Swagger2Markup / outputDirectory := target.value / "docs",
    Swagger2Markup / properties := Map("markupLanguage" -> "MARKDOWN"),
    name := "swagger-to-markdown",
    scalaVersion := "2.12.8",
    version := "0.1",
    logLevel := Level.Debug
  )
