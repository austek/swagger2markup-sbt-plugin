lazy val root = (project in file("."))
  .enablePlugins(Swagger2MarkupPlugin)
  .settings(
    swagger2MarkupInput := "src/main/swagger/swagger.json",
    swagger2MarkupOutputDirectory := target.value / "docs",
    swagger2MarkupProperties := Map("swagger2markup.markupLanguage" -> "MARKDOWN"),
    name := "swagger-to-markdown"
  )
