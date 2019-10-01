lazy val root = (project in file("."))
  .enablePlugins(Swagger2MarkupPlugin)
  .settings(
    Swagger2Markup / swaggerInput := "src/main/swagger/swagger.json",
    Swagger2Markup / outputDirectory := target.value / "docs",
    Swagger2Markup / properties := Map("swagger2markup.markupLanguage" -> "MARKDOWN"),
    name := "swagger-to-markdown"
  )
