lazy val root = (project in file("."))
  .enablePlugins(OpenApi2MarkupPlugin)
  .settings(
    openApi2MarkupOutputDirectory := target.value / "generated-docs" / "open_api",
    skip := true,
    name := "skip-execution"
  )
