package com.xonal.server

import fs2.Stream
import cats.effect.{IO,Resource}
import cats.effect.std.Console
import org.http4s.HttpRoutes
import org.http4s.ember.server.EmberServerBuilder
import cats.syntax.all.*
import cats.effect.Async
import com.comcast.ip4s.{Port,Host}
import org.http4s.server.Server
import com.xonal.config.Config

object ServerUtil:
    def oauthServer[F[_]: Async: Console](config: Config, service: HttpRoutes[F]): Stream[F, Resource[F, Server]] = 
        for
            _ <- Stream.eval(Console[F].println("Starting server"))
            server <- Stream.apply(EmberServerBuilder
                .default[F]
                .withHost(config.server.hostValue)
                .withPort(config.server.portValue)
                .withHttpApp(service.orNotFound)
                .build)
        yield server