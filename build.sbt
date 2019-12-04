name := """GradeNetScala"""
version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

resolvers += Resolver.sonatypeRepo("snapshots")

scalaVersion := "2.13.0"

resolvers += "Local Ivy Repository" at "file:///" + Path.userHome.absolutePath + "/.ivy2/local"

libraryDependencies += guice
libraryDependencies += "com.h2database" % "h2" % "1.4.199"
libraryDependencies += "com.lihaoyi" %% "requests" % "0.2.0"

scalacOptions ++= Seq(
  "-feature",
  "-deprecation",
  "-Xfatal-warnings"
)
