lazy val root = (project in file("."))
  .enablePlugins(OpenApi2MarkupPlugin)
  .settings(
    openApi2MarkupInput := "src/docs",
    name := "mirror-input-directory"
  )
