package com.xonal

import cats.effect.*
import com.xonal.server.ServerUtil
import fs2.Stream
import com.xonal.config.configuration
import com.xonal.routes.GithubRoutes.githubRoutes
import org.http4s.server.Server

object OauthMain extends IOApp {
  def program:IO[Resource[IO, Server]] =
    for
      config <- configuration.load[IO]
      server <- IO.apply(ServerUtil.oauthServer[IO](config, githubRoutes(config)))
    yield server
    
  def run(args: List[String]): IO[ExitCode] = program.flatMap(_.use(_ => IO.never)).as(ExitCode.Success)
}