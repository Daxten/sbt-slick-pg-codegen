sbtPlugin := true

name := """sbt-slick-pg-codegen"""
organization := "de.daxten"
scalaVersion := "2.12.3"
version := "0.2-SNAPSHOT"

val slick         = "3.2.1" // http://slick.lightbend.com/
val slickPg       = "0.15.3" // https://github.com/tminglei/slick-pg
val flyway        = "4.2.0" // https://flywaydb.org/documentation/api/
val scalaMeta     = "1.7.0" // https://github.com/scalameta/scalameta
val scalaFmt      = "1.2.0" // https://github.com/olafurpg/scalafmt
val swaggerParser = "1.0.31" // https://github.com/swagger-api/swagger-parser
val betterFiles   = "3.0.+" // https://github.com/pathikrit/better-files
val postgres      = "42.1.4"

libraryDependencies ++= Seq(
  "com.typesafe.slick"   %% "slick-codegen"       % slick,
  "com.typesafe.slick"   %% "slick-codegen"       % slick,
  "org.scalameta"        %% "scalameta"           % scalaMeta,
  "com.geirsson"         %% "scalafmt-core"       % scalaFmt,
  "com.github.pathikrit" %% "better-files"        % betterFiles,
  "org.postgresql"       % "postgresql"           % postgres,
  "org.flywaydb"         % "flyway-core"          % flyway,
  "io.swagger"           % "swagger-parser"       % swaggerParser
)

publishArtifact in Test := false
bintrayRepository := "maven"
publishMavenStyle := true