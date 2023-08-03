package com.xonal.server

import cats.effect.Async
import fs2.Stream
import com.xonal.config.Config
import org.http4s.HttpRoutes
import cats.effect.Resource
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Server

object ServerUtil {
  def oauthServer[F[_]: Async](
      config: Config,
      service: HttpRoutes[F]
  ): Resource[F, Server] =
    EmberServerBuilder
      .default[F]
      .withHost(config.server.hostValue)
      .withPort(config.server.portValue)
      .withHttpApp(service.orNotFound)
      .build
}