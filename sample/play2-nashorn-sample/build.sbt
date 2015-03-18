name := """play2-nashorn-sample"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala).settings(
  unmanagedResourceDirectories in Compile += baseDirectory.value / "resources"
)

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  ws,
 "jp.co.bizreach"               %% "play2-nashorn"      % "0.1-SNAPSHOT"
)

// ???
//unmanagedResourceDirectories in Compile += baseDirectory.value / "resources"

//includeFilter in (Compile, unmanagedResourceDirectories) := "*.js"