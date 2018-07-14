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

// Slick/RDBMS/MySQL dependencies
libraryDependencies ++= Seq(
  "com.typesafe.slick" %% "slick" % "3.2.1",
  "org.slf4j" % "slf4j-nop" % "1.6.4",
  "com.typesafe.slick" %% "slick-hikaricp" % "3.2.1",
  "mysql" % "mysql-connector-java" % "latest.release",
  "com.h2database" % "h2" % "1.4.187"
)