name := """GradeNetScala"""
version := "1.0-SNAPSHOT"
scalaVersion := "2.12.8"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

libraryDependencies += guice
libraryDependencies += jdbc
libraryDependencies += "com.h2database" % "h2" % "1.4.199"
libraryDependencies += "com.lihaoyi" %% "requests" % "0.2.0"
libraryDependencies += "mysql" % "mysql-connector-java" % "5.1.41"
libraryDependencies += "org.postgresql" % "postgresql" % "9.4.1208"
libraryDependencies += "org.playframework.anorm" %% "anorm" % "2.6.2"

scalacOptions ++= Seq(
  "-feature",
  "-deprecation",
  "-Xfatal-warnings"
)
