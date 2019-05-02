lazy val root = (project in file("."))
  .enablePlugins(Swagger2MarkupPlugin)
  .settings(
    Swagger2Markup / outputDirectory := target.value / "generated-docs" / "swagger",
    Swagger2Markup / skip := true,
    name := "skip-execution"
  )
