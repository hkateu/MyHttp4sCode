val scala3Version = "3.3.0"

val Http4sVersion = "0.23.18"
val MunitVersion = "0.7.29"
val LogbackVersion = "1.2.6"
val MunitCatsEffectVersion = "1.0.6"
val CirceVersion = "0.14.1"
val CirceExtras = "0.14.3"
val BcryptVersion = "0.10.2"
val JwtScalaVersion = "9.3.0"
val JwtHttp4sVersion = "1.2.0"
val OtpJavaVersion = "2.0.1"
val ZxingVersion = "3.5.1"
val SendGridVersion = "4.9.3"
val CirisVersion = "3.2.0"
val Log4CatsVersion = "2.6.0"
val Sl4jApiVersion =  "2.0.7"

val emberServer =     "org.http4s"                %% "http4s-ember-server" % Http4sVersion
val emberClient =     "org.http4s"                %% "http4s-ember-client" % Http4sVersion
val http4sCirce =     "org.http4s"                %% "http4s-circe"        % Http4sVersion
val http4sDsl =       "org.http4s"                %% "http4s-dsl"          % Http4sVersion
val munit =           "org.scalameta"             %% "munit"               % MunitVersion  % Test 
val munitCatsEffect3 ="org.typelevel"             %% "munit-cats-effect-3" % MunitCatsEffectVersion % Test
val logbackClassic =  "ch.qos.logback"             % "logback-classic"     % LogbackVersion
val bcrypt =          "at.favre.lib"               % "bcrypt"              % BcryptVersion
val jwtScala =        "com.github.jwt-scala"      %% "jwt-core"            % JwtScalaVersion
val jwtCirce =        "com.github.jwt-scala"      %% "jwt-circe"           % JwtScalaVersion
val jwtHttp4s =       "dev.profunktor"            %% "http4s-jwt-auth"     % JwtHttp4sVersion
val otpJava =         "com.github.bastiaanjansen"  % "otp-java"            % OtpJavaVersion
val zxing =           "com.google.zxing"           % "javase"              % ZxingVersion
val sendGrid =        "com.sendgrid"               % "sendgrid-java"       % SendGridVersion
val ciris =           "is.cir"                    %% "ciris"               % CirisVersion
val cirisCirce =      "is.cir"                    %% "ciris-circe"         % CirisVersion
val log4CatsCore =    "org.typelevel"             %% "log4cats-core"       % Log4CatsVersion
val log4CatsSlf4j =   "org.typelevel"             %% "log4cats-slf4j"      % Log4CatsVersion
val sl4jApi =         "org.slf4j"                  % "slf4j-api"           % Sl4jApiVersion
val sl4jSimple =      "org.slf4j"                  % "slf4j-simple"        % Sl4jApiVersion
val circeLibs =  Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % CirceVersion)

Compile / run / mainClass := Some("Main")

lazy val root = (project in file("."))
  .aggregate(main,service,dsl,middleware,authentication,otpauth,gzip,hsts,
  staticFiles,httpClient,entityHandling,streaming,jsonHandling,testing
  ,urihandling,httpmethods,errorhandling,smiddleware,oauth)

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
      bcrypt,
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
      http4sCirce,
      http4sDsl,
      jwtScala,
      jwtCirce,
      jwtHttp4s,
      otpJava,
      zxing,
      emberServer,
      emberClient,
      ciris,
      cirisCirce,
      munit
    ) ++ circeLibs
  )

  lazy val otpauth = project
  .in(file("otpauth"))
  .settings(
    name := "othauth",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,
    scalacOptions ++= Seq("-java-output-version", "11"),
    libraryDependencies ++= Seq(
      http4sDsl,
      otpJava,
      zxing,
      emberServer,
      emberClient,
      sendGrid,
      log4CatsCore,
      log4CatsSlf4j,
      sl4jApi,
      sl4jSimple
    )
  )

  lazy val gzip = project
  .in(file("gzip"))
  .settings(
    name := "gzip",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,
    libraryDependencies ++= Seq(
      emberServer,
      http4sCirce,
      http4sDsl,
      bcrypt,
      munit
    ) ++ circeLibs
  )

  lazy val hsts = project
  .in(file("hsts"))
  .settings(
    name := "hsts",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,
    libraryDependencies ++= Seq(
      emberServer,
      http4sCirce,
      http4sDsl,
      munit
    ) ++ circeLibs
  )

  lazy val staticFiles = project
  .in(file("staticFiles"))
  .settings(
    name := "staticFiles",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,
    libraryDependencies ++= Seq(
      emberServer,
      http4sCirce,
      http4sDsl,
      munit
    ) ++ circeLibs
  )

  lazy val httpClient = project
  .in(file("httpClient"))
  .settings(
    name := "httpClient",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,
    libraryDependencies ++= Seq(
      emberServer,
      emberClient,
      http4sCirce,
      http4sDsl,
      munit
    ) ++ circeLibs
  )

  lazy val entityHandling = project
  .in(file("entityHandling"))
  .settings(
    name := "entityHandling",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,
    libraryDependencies ++= Seq(
      emberServer,
      emberClient,
      http4sCirce,
      http4sDsl,
      munit
    ) ++ circeLibs
  )

  lazy val streaming = project
  .in(file("streaming"))
  .settings(
    name := "streaming",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,
    libraryDependencies ++= Seq(
      emberServer,
      emberClient,
      http4sCirce,
      http4sDsl,
      munit
    ) ++ circeLibs
  )

lazy val jsonHandling = project
  .in(file("jsonHandling"))
  .settings(
    name := "jsonHandling",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,
    libraryDependencies ++= Seq(
      emberServer,
      emberClient,
      http4sCirce,
      http4sDsl,
      munit
    ) ++ circeLibs
  )

lazy val testing = project
  .in(file("testing"))
  .settings(
    name := "testing",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,
    libraryDependencies ++= Seq(
      emberServer,
      emberClient,
      http4sCirce,
      http4sDsl,
      munit
    ) ++ circeLibs
  )

lazy val urihandling = project
  .in(file("urihandling"))
  .settings(
    name := "urihandling",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,
    libraryDependencies ++= Seq(
      emberServer,
      emberClient,
      http4sCirce,
      http4sDsl,
      munit
    ) ++ circeLibs
  )

lazy val httpmethods = project
  .in(file("httpmethods"))
  .settings(
    name := "httpmethods",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,
    libraryDependencies ++= Seq(
      emberServer,
      emberClient,
      http4sCirce,
      http4sDsl,
      munit
    ) ++ circeLibs
  )

lazy val errorhandling = project
  .in(file("errorhandling"))
  .settings(
    name := "errorhandling",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,
    libraryDependencies ++= Seq(
      emberServer,
      emberClient,
      http4sCirce,
      http4sDsl,
      munit
    ) ++ circeLibs
  )

lazy val smiddleware = project
  .in(file("smiddleware"))
  .settings(
    name := "smiddleware",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,
    libraryDependencies ++= Seq(
      emberServer,
      emberClient,
      http4sCirce,
      http4sDsl,
      munit
    ) ++ circeLibs
  )

  lazy val oauth = project
  .in(file("oauth"))
  .settings(
    name := "oauth",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,
    libraryDependencies ++= Seq(
      emberServer,
      emberClient,
      http4sDsl,
      http4sCirce,
      ciris,
      cirisCirce,
      logbackClassic
    ) ++ circeLibs
  )
