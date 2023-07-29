package com.xonal

import cats.effect.*
import com.xonal.server.ServerUtil
import fs2.Stream
import com.xonal.config.configuration
import com.xonal.routes.GithubRoutes.githubRoutes
import org.http4s.server.Server

object OauthMain extends IOApp {
  def program[F[_]: Async]: Stream[F, Resource[F, Server]] =
    for
      sconfig <- Stream.eval(configuration.load[F])
      server <- ServerUtil.oauthServer(sconfig, githubRoutes)
    yield server
    
  def run(args: List[String]): IO[ExitCode] =
    program[IO].compile.last
      .flatMap(_.get.use(_ => IO.never))
      .as(ExitCode.Success)
}
