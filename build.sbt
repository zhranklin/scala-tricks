lazy val commonSettings: Seq[Def.Setting[_]] = Seq(
  organization := "com.zhranklin",
  version := "0.1.0-SNAPSHOT",
  scalaVersion := "2.11.8",
  crossScalaVersions := Seq(scalaVersion.value, "2.12.1"),
  // Sonatype OSS deployment
  publishMavenStyle := true,
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases"  at nexus + "service/local/staging/deploy/maven2")
  },
  publishArtifact in Test := false,
  pomIncludeRepository := { _ => false },
  licenses := ("Apache2", url("http://www.apache.org/licenses/LICENSE-2.0.txt")) :: Nil,
  homepage := Some(url("http://softwaremill.com")),
  scmInfo := Some(ScmInfo(url("https://github.com/zhranklin/scala-tricks"), "scm:git:git@github.com/zhranklin/scala-tricks.git", None)),
  developers := Developer("Zhranklin", "Zhranklin", "chigou79@outlook.com", url("http://www.zhranklin.com")) :: Nil
)

lazy val rootProject = (project in file("."))
  .settings(
    commonSettings,
    publishArtifact := false,
    name := "scala-tricks-root")
  .aggregate(core)

lazy val core = project.settings(
  commonSettings,
  name := "scala-tricks"
)
