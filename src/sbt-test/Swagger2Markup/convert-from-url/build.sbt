lazy val root = (project in file("."))
  .enablePlugins(Swagger2MarkupPlugin)
  .settings(
    swagger2MarkupInput := "http://petstore.swagger.io/v2/swagger.json",
    name := "convert-from-url"
  )
