lazy val root = (project in file("."))
  .enablePlugins(Swagger2MarkupPlugin)
  .settings(
    swagger2MarkupInput := "src/docs/swagger/swagger.json",
    name := "convert-into-directory"
  )
