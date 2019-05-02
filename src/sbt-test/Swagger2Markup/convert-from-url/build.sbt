lazy val root = (project in file("."))
  .enablePlugins(Swagger2MarkupPlugin)
  .settings(
    Swagger2Markup / swaggerInput := "http://petstore.swagger.io/v2/swagger.json",
    name := "convert-from-url"
  )
