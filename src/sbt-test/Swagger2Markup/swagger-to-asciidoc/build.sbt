lazy val root = (project in file("."))
  .enablePlugins(Swagger2MarkupPlugin)
  .settings(
    Swagger2Markup / swaggerInput := "src/docs/swagger",
    Swagger2Markup / outputDirectory := target.value / "docs",
    Swagger2Markup / properties := Map("swagger2markup.markupLanguage" -> "ASCIIDOC"),
    name := "swagger-to-asciidoc"
  )
