lazy val commonSettings: Seq[Def.Setting[_]] = Seq(
  organization := "com.zhranklin",
  version := "0.2.11",
  scalaVersion := "3.7.3",
  // Sonatype OSS deployment
  publishMavenStyle := true,
  publishTo := {
    val centralSnapshots = "https://central.sonatype.com/repository/maven-snapshots/"
    if (isSnapshot.value) Some("central-snapshots" at centralSnapshots)
    else localStaging.value
  },
  Test / publishArtifact := false,
  pomIncludeRepository := { _ => false },
  licenses := ("Apache2", url("http://www.apache.org/licenses/LICENSE-2.0.txt")) :: Nil,
  homepage := Some(url("http://zhranklin.com")),
  scmInfo := Some(ScmInfo(url("https://github.com/zhranklin/scala-tricks"), "scm:git:git@github.com/zhranklin/scala-tricks.git", None)),
  developers := Developer("Zhranklin", "Zhranklin", "chigou79@outlook.com", url("http://www.zhranklin.com")) :: Nil,
  credentials += Credentials(Path.userHome / ".sbt" / ".credentials")
)

lazy val rootProject = (project in file("."))
  .settings(
    commonSettings,
    publishArtifact := false,
    name := "scala-tricks-root")
  .aggregate(core)

lazy val core = project.settings(
  commonSettings,
  name := "scala-tricks",
  libraryDependencies ++=
    "com.lihaoyi"    %% "os-lib"     % "0.11.6"  ::
    "me.vican.jorge" %  "dijon_2.13" % "0.6.0"   ::
    "com.lihaoyi"    %% "requests"   % "0.9.0"   ::
    "io.reactivex.rxjava3" % "rxjava" % "3.1.12" % "provided" ::
    "com.volcengine" % "ve-tos-java-sdk" % "2.9.8" % "provided" ::
    "com.aliyun" % "alibabacloud-oss-v2" % "0.3.0" % "provided" ::
    Nil
)
