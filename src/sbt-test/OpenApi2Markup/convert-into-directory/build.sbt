lazy val root = (project in file("."))
  .enablePlugins(OpenApi2MarkupPlugin)
  .settings(
    openApi2MarkupInput := "src/docs/open_api/petstore.yaml",
    name := "convert-into-directory"
  )
