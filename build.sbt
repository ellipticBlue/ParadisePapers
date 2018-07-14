name := "ParadisePapers"

version := "0.1"

scalaVersion := "2.11.12"

// Dependencies for tests
libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.0.3" % "test"
)

// Akka dependencies for actors and HTTP server
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.5.9",
  "com.typesafe.akka" %% "akka-stream" % "2.5.9",
  "com.typesafe.akka" %% "akka-http" % "10.1.0-RC2",
  "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.0-RC2"
)