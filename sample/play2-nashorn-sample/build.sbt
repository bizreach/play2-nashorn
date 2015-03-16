name := """play2-nashorn-sample"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  ws,
 "jp.co.bizreach"               %% "play2-nashorn"      % "0.1-SNAPSHOT"
)
