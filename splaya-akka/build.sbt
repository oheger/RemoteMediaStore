lazy val root = (project in file(".")).
  settings(
    name := "splaya-akka",
    version := "1.1-SNAPSHOT",
    scalaVersion := "2.11.4",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor" % "2.3.4",
      "com.typesafe.akka" %% "akka-testkit" % "2.3.4",
      "org.scalatest" %% "scalatest" % "2.1.6" % "test",
      "junit" % "junit" % "4.12" % "test",
      "org.mockito" % "mockito-core" % "1.9.5" % "test"
    )
  )