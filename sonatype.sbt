sonatypeProfileName := "com.mdsol"
publishMavenStyle := true
licenses := Seq("MDSOL" -> url("https://github.com/mdsol/swagger2markup-sbt-plugin/blob/master/LICENSE"))
scmInfo := Some(
  ScmInfo(
    url("https://github.com/mdsol/swagger2markup-sbt-plugin"),
    "scm:git@github.com:mdsol/swagger2markup-sbt-plugin.git"
  )
)

developers := List(
  Developer(id = "austek", name = "Ali Ustek", email = "austek@mdsol.com", url = url("https://github.com/austek"))
)

import xerial.sbt.Sonatype._
sonatypeProjectHosting := Some(GitHubHosting("austek", "sbt-swagger2markup", "austek@mdsol.com"))

publishTo := sonatypePublishToBundle.value
releaseCommitMessage := s"Setting version to ${(version in ThisBuild).value} [ci skip]"
releasePublishArtifactsAction := PgpKeys.publishSigned.value
releaseVersionBump := sbtrelease.Version.Bump.Bugfix

releaseCrossBuild := false // true if you cross-build the project for multiple Scala versions
import sbtrelease.ReleaseStateTransformations._
releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  // For cross-build projects, use releaseStepCommand("+publishSigned")
  releaseStepCommandAndRemaining("publishSigned"),
  releaseStepCommand("sonatypeBundleRelease"),
  setNextVersion,
  commitNextVersion,
  pushChanges
)
credentials += Credentials(
  "GnuPG Key ID",
  "pgp",
  "946D8A2EE7D64157DFF9564C8C712BD6783994A5",
  "ignored"
)
