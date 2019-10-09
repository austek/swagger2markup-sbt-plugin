lazy val root = (project in file("."))
  .enablePlugins(Swagger2MarkupPlugin)
  .settings(
    swagger2MarkupInput := "src/docs/swagger/swagger.json",
    swagger2MarkupOutputFile := Some(target.value / "generated-docs" / "swagger"),
    name := "convert-into-file"
  )
