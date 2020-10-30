lazy val root = (project in file("."))
  .enablePlugins(OpenApi2MarkupPlugin)
  .settings(
    openApi2MarkupInput := "src/docs/open_api",
    openApi2MarkupOutputDirectory := target.value / "docs",
    openApi2MarkupProperties := Map("openApi2markup.markupLanguage" -> "ASCIIDOC"),
    name := "openApi-to-asciidoc"
  )
