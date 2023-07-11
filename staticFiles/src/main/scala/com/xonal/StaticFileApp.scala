package com.xonal
import cats.effect.*
import com.comcast.ip4s
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Server
import org.http4s.server.staticcontent.*
import com.comcast.ip4s.*
import org.http4s.*
import org.http4s.server.Router
import org.http4s.dsl.io.*
import fs2.io.file.Path

object StaticFileApp extends IOApp:
    val httpApp: HttpApp[IO] = 
        Router(
            "/" -> fileService[IO](FileService.Config("staticFiles/src/main/resources/test.txt"))
        ).orNotFound

    //inline in Route
    val routes = HttpRoutes.of[IO]{
        case request @ GET -> Root / "index.html" =>
            StaticFile.fromPath(Path("staticFiles/src/main/resources/index.html"),Some(request)).getOrElseF(NotFound())
    }.orNotFound

    val assetsRoutes = resourceServiceBuilder[IO]("/assets").toRoutes

    def static(file: String, request: Request[IO]) = 
        StaticFile.fromResource("/" + file, Some(request)).getOrElseF(NotFound())

    val fileTypes = List(".js", ".css", ".map", ".html", ".webm")

    val fileRoutes = HttpRoutes.of[IO]{
        case request @ GET -> Root / path if fileTypes.exists(path.endsWith) => 
            static(path, request)
    }.orNotFound

    val app: Resource[IO,Server] =
        EmberServerBuilder
            .default[IO]
            .withHost(ipv4"0.0.0.0")
            .withPort(port"8080")
            .withHttpApp(routes)
            .build

    def run(args: List[String]): IO[ExitCode] = app.use(_ => IO.never).as(ExitCode.Success)