val scala3Version = "3.3.1"

lazy val root = project
  .in(file("."))
  .settings(
    name := "fs2udp",
    version := "0.1.0-SNAPSHOT",

    scalaVersion := scala3Version,

    libraryDependencies += "co.fs2" %% "fs2-core" % "3.9.2",
    libraryDependencies += "co.fs2" %% "fs2-io" % "3.9.2",
    libraryDependencies += "org.scalameta" %% "munit" % "0.7.29" % Test
  )
