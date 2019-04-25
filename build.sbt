useGpg := false
usePgpKeyHex("7D16F4CEB1F2979C")
pgpPublicRing := baseDirectory.value / "travis" / ".gnupg" / "pubring.asc"
pgpSecretRing := baseDirectory.value / "travis" / ".gnupg" / "secring.asc"
pgpPassphrase := sys.env.get("PGP_PASS").map(_.toArray)

lazy val root = (project in file("."))
  .enablePlugins(SbtPlugin)
  .settings(
    organization := "com.mdsol",
    name := "sbt-swagger2markup",
    description := "Swagger2Markup SBT Plugin",
    scalaVersion := "2.12.8",
    libraryDependencies ++= Seq(
      "io.github.swagger2markup" % "swagger2markup" % "1.3.3"
    ),
    resolvers += Resolver.mavenLocal,
    resolvers += Resolver.sonatypeRepo("releases"),
    resolvers += Resolver.jcenterRepo,
    javacOptions ++= Seq("-encoding", "UTF-8"),
    scalacOptions := Seq(
      "-encoding",
      "utf8",
      "-feature",
      "-unchecked",
      "-deprecation",
      "-target:jvm-1.8",
      "-language:_",
      "-Xlint",
      "-Xlog-reflective-calls",
      "-Ywarn-adapted-args",
      "-Ywarn-unused",
      "-Ywarn-unused-import"
    )
  )
