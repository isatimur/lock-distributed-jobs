name := "news"

version := "1.0"

lazy val `news` = (project in file(".")).enablePlugins(PlayJava, PlayScala)

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

resolvers += "Akka Snapshot Repository" at "http://repo.akka.io/snapshots/"

scalaVersion := "2.12.4"

libraryDependencies ++= Seq(jdbc, ehcache, ws, specs2 % Test, guice)

unmanagedResourceDirectories in Test <+= baseDirectory(_ / "target/web/public/test")

unmanagedResourceDirectories in Compile <+= baseDirectory( _ / "conf")
      