lazy val root = (project in file("."))
  .enablePlugins(SbtPlugin)
  .settings(
    organization := "com.mdsol",
    name := "sbt-swagger2markup",
    description := "Swagger2Markup SBT Plugin",
    scalaVersion := "2.12.10",
    libraryDependencies ++= Seq(
      "io.github.swagger2markup" % "swagger2markup" % "1.3.3"
    ),
    scriptedLaunchOpts ++= Seq(s"-Dplugin.version=${version.value}"),
    scriptedBufferLog := false,
    resolvers += Resolver.mavenLocal,
    resolvers += Resolver.sonatypeRepo("releases"),
    resolvers += Resolver.jcenterRepo,
    javacOptions ++= Seq("-encoding", "UTF-8"),
    scalacOptions := Seq(
      "-deprecation",
      "-encoding",
      "utf-8",
      "-explaintypes",
      "-feature",
      "-language:existentials",
      "-language:experimental.macros",
      "-language:higherKinds",
      "-language:implicitConversions",
      "-language:reflectiveCalls",
      "-language:postfixOps",
      "-unchecked",
      "-Xcheckinit",
      "-Xfatal-warnings",
      "-Xfuture",
      "-Xlint:adapted-args",
      "-Xlint:by-name-right-associative",
      "-Xlint:constant",
      "-Xlint:delayedinit-select",
      "-Xlint:doc-detached",
      "-Xlint:inaccessible",
      "-Xlint:infer-any",
      "-Xlint:missing-interpolator",
      "-Xlint:nullary-override",
      "-Xlint:nullary-unit",
      "-Xlint:option-implicit",
      "-Xlint:package-object-classes",
      "-Xlint:poly-implicit-overload",
      "-Xlint:private-shadow",
      "-Xlint:stars-align",
      "-Xlint:type-parameter-shadow",
      "-Xlint:unsound-match",
      "-Yno-adapted-args",
      "-Ypartial-unification",
      "-Ywarn-dead-code",
      "-Ywarn-inaccessible",
      "-Ywarn-infer-any",
      "-Ywarn-nullary-override",
      "-Ywarn-nullary-unit",
      "-Ywarn-numeric-widen"
    )
  )
