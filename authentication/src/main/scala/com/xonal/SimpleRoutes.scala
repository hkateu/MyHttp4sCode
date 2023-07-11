package com.xonal

import cats.effect.*
import org.http4s.*
import org.http4s.dsl.io.*
import org.http4s.server.*
import org.http4s.implicits.*
import org.http4s.ember.server.*
import com.comcast.ip4s.*

object SimpleRoutes extends IOApp:
    val routes: HttpRoutes[IO] =
        HttpRoutes.of {
            case GET -> Root / "welcome" / user =>
                Ok(s"Welcome, ${user}")
        }

    val server = EmberServerBuilder
        .default[IO]
        .withHost(ipv4"0.0.0.0")
        .withPort(port"8080")
        .withHttpApp(routes.orNotFound)
        .build

    override def run(args: List[String]): IO[ExitCode] = server.use(_ => IO.never).as(ExitCode.Success)

