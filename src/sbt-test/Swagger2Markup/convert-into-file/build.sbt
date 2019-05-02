lazy val root = (project in file("."))
  .enablePlugins(Swagger2MarkupPlugin)
  .settings(
    Swagger2Markup / swaggerInput := "src/docs/swagger/swagger.json",
    Swagger2Markup / outputFile := Some(target.value / "generated-docs" / "swagger"),
    name := "convert-into-file",
    logLevel := Level.Debug
  )
