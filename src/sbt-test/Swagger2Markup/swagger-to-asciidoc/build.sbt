lazy val root = (project in file("."))
  .enablePlugins(Swagger2MarkupPlugin)
  .settings(
    Swagger2Markup / swaggerInput := "src/docs/swagger",
    Swagger2Markup / outputDirectory := target.value / "docs",
    Swagger2Markup / attributes := Map("markupLanguage" -> "asciidoc"),
    name := "swagger-to-asciidoc",
    scalaVersion := "2.12.8",
    version := "0.1"
  )
