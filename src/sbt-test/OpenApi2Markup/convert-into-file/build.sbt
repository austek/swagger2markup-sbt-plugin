lazy val root = (project in file("."))
  .enablePlugins(OpenApi2MarkupPlugin)
  .settings(
    openApi2MarkupInput := "src/docs/open_api/petstore.yaml",
    openApi2MarkupOutputFile := Some(target.value / "generated-docs" / "petstore"),
    name := "convert-into-file"
  )
