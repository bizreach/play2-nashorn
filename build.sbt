
name := "play2-nashorn"

organization := "jp.co.bizreach"

version := "0.2.0"

scalaVersion := "2.11.7"

// The minimum compiler version is 1.8 since it requires Nashorn
javacOptions ++= Seq("-source", "1.8")

resolvers ++= Seq(
  "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
  "Maven Central Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
)

libraryDependencies ++= Seq(
  "com.typesafe.play"           %% "play"                   % "2.4.2"     % "provided",
  "com.netaporter"              %% "scala-uri"              % "0.4.6",
  "org.json4s"                  %% "json4s-core"            % "3.2.11",
  "org.json4s"                  %% "json4s-jackson"         % "3.2.11",
  "com.typesafe.scala-logging"  %% "scala-logging"          % "3.1.0",
  "ch.qos.logback"               % "logback-classic"        % "1.1.2",
  "org.scalatest"               %% "scalatest"              % "2.2.4"     % "test",
  "com.typesafe.play"           %% "play-test"              % "2.4.2"     % "test",
  "org.mockito"                  % "mockito-all"            % "1.10.19"   % "test"
)

publishMavenStyle := true

publishTo := {
  if (isSnapshot.value)
    Some("snapshots" at "https://oss.sonatype.org/content/repositories/snapshots")
  else
    Some("releases" at "https://oss.sonatype.org/service/local/staging/deploy/maven2")
}

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

scalacOptions := Seq("-deprecation", "-feature")

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

pomExtra := (
  <url>https://github.com/bizreach/play2-nashorn</url>
    <licenses>
      <license>
        <name>The Apache Software License, Version 2.0</name>
        <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
      </license>
    </licenses>
    <scm>
      <url>https://github.com/bizreach/play2-nashorn</url>
      <connection>scm:git:https://github.com/bizreach/play2-nashorn.git</connection>
    </scm>
    <developers>
      <developer>
        <id>scova0731</id>
        <name>Satoshi Kobayashi</name>
        <email>satoshi.kobayashi_at_bizreach.co.jp</email>
        <timezone>+9</timezone>
      </developer>
    </developers>)
