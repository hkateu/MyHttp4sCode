package com.xonal

import cats.effect.{IO, IOApp, ExitCode}
import org.http4s.server.middleware.{ErrorAction, ErrorHandling}
import org.http4s.*
import org.http4s.dsl.io.*
import org.http4s.ember.server.EmberServerBuilder
import com.comcast.ip4s.*
import cats.data.OptionT


object MyErrorHandling extends IOApp:

    val errorRoute: HttpRoutes[IO] = HttpRoutes.of[IO]{
        case req @ GET -> Root / "error" =>
            throw new Exception("Hey don't swallow me")
    }

    val withErrorLogging = ErrorHandling.Recover.total(
        ErrorAction.log(
            errorRoute, 
            messageFailureLogAction = (t,msg) => 
                OptionT.liftF[IO,Unit](IO.println(t)).map(_ => IO.println(msg)),
            serviceErrorLogAction = (t,msg) =>
                OptionT.liftF[IO,Unit](IO.println(t)).map(_ => IO.println(msg))
        )
    ).orNotFound

    val server = EmberServerBuilder
        .default[IO]
        .withPort(port"8081")
        .withHost(host"localhost")
        .withHttpApp(withErrorLogging)
        .build

    def run(args: List[String]): IO[ExitCode] = server.use(_ => IO.never).as(ExitCode.Success)
