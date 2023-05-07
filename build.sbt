val scala3Version = "3.2.2"

val Http4sVersion = "0.23.18"
val MunitVersion = "0.7.29"
val LogbackVersion = "1.2.6"
val MunitCatsEffectVersion = "1.0.6"
val CirceVersion = "0.14.1"
val CirceExtras = "0.14.3"

val emberServer =     "org.http4s"      %% "http4s-ember-server" % Http4sVersion
val emberClient =     "org.http4s"      %% "http4s-ember-client" % Http4sVersion
val http4sCirce =     "org.http4s"      %% "http4s-circe"        % Http4sVersion
val http4sDsl =       "org.http4s"      %% "http4s-dsl"          % Http4sVersion
val munit =           "org.scalameta"   %% "munit"               % MunitVersion  % Test 
val munitCatsEffect3 ="org.typelevel"   %% "munit-cats-effect-3" % MunitCatsEffectVersion % Test
val logbackClassic =  "ch.qos.logback"  %  "logback-classic"     % LogbackVersion
val circeLibs =  Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % CirceVersion)

Compile / run / mainClass := Some("Main")

lazy val root = (project in file("."))
  .aggregate(main,service,dsl)

lazy val main = project
  .in(file("main"))
  .settings(
    name := "main",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,
    libraryDependencies += munit
  )

lazy val service = project
  .in(file("service"))
  .settings(
    name := "service",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,
    libraryDependencies ++= Seq(
      emberServer,
      http4sCirce,
      http4sDsl,
      munit
    ) ++ circeLibs
  )

  lazy val dsl = project
  .in(file("dsl"))
  .settings(
    name := "dsl",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,
    libraryDependencies ++= Seq(
      emberServer,
      http4sCirce,
      http4sDsl,
      munit
    ) ++ circeLibs
  )

  lazy val middleware = project
  .in(file("middleware"))
  .settings(
    name := "middleware",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,
    libraryDependencies ++= Seq(
      emberServer,
      http4sCirce,
      http4sDsl,
      munit
    ) ++ circeLibs
  )

  lazy val authentication = project
  .in(file("authentication"))
  .settings(
    name := "authentication",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,
    libraryDependencies ++= Seq(
      emberServer,
      http4sCirce,
      http4sDsl,
      munit
    ) ++ circeLibs
  )