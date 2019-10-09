lazy val root = (project in file("."))
  .enablePlugins(Swagger2MarkupPlugin)
  .settings(
    swagger2MarkupInput := "src/docs",
    name := "mirror-input-directory"
  )
