lazy val root = (project in file("."))
  .enablePlugins(Swagger2MarkupPlugin)
  .settings(
    swagger2MarkupInput := "src/docs/swagger",
    swagger2MarkupOutputDirectory := target.value / "docs",
    swagger2MarkupProperties := Map("swagger2markup.markupLanguage" -> "ASCIIDOC"),
    name := "swagger-to-asciidoc"
  )
