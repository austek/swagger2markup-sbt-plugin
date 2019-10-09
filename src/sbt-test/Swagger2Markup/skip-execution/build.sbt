lazy val root = (project in file("."))
  .enablePlugins(Swagger2MarkupPlugin)
  .settings(
    swagger2MarkupOutputDirectory := target.value / "generated-docs" / "swagger",
    skip := true,
    name := "skip-execution"
  )
