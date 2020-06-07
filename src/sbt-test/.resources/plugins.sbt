sys.props.get("plugin.version") match {
  case Some(x) => addSbtPlugin("com.mdsol" % "sbt-swagger2markup" % x)
  case _       => sys.error("""|The system property 'plugin.version' is not defined.
                         |Specify this property using the scriptedLaunchOpts -D.""".stripMargin)
}

resolvers ++= Seq(
  Resolver.mavenLocal,
  "JFrog Artifactory" at "https://oss.jfrog.org/artifactory/oss-snapshot-local/",
  Resolver.jcenterRepo
)

// Used to clean cache files for the libraries published by the test artifact
// before each run, as cached test artifacts from previous tests in ~/.ivy2/cache will otherwise interfere
// with the test itself (e.g. resolving the wrong artifact when checking binary compatibility)
addSbtPlugin("com.eed3si9n" % "sbt-dirty-money" % "0.2.0")
