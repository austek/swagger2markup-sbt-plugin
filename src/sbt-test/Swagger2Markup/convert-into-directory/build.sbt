lazy val root = (project in file("."))
  .enablePlugins(Swagger2MarkupPlugin)
  .settings(
    Swagger2Markup / swaggerInput := "src/docs/swagger/swagger.json",
    name := "convert-into-directory",
    logLevel := Level.Debug
  )
