package com.xonal

import cats.effect.* 
import cats.effect.std.Console
import com.xonal.server.ServerUtil
import fs2.Stream
import com.xonal.config.{serverConfig, apiConfig}
import cats.syntax.all.*
import com.xonal.routes.GithubRoutes.githubRoutes
import org.http4s.server.Server
import cats.effect.kernel.Async

object Main extends IOApp:
    // val CONSUMER_KEY = "f49e598e44ee5a7c8f58"
    // val CONSUMER_SECRET = "3eff0b277d1823fa5edcc71c690b1cb699716337"

    // https://github.com/login/oauth/authorize?scope=user:email&client_id=

    IO.never
    def program[F[_]: Async: Console]: Stream[F, Resource[F, Server]] = 
        for
            - <- Stream.eval(Console[F].println("Program Starting"))
            sConf <- Stream.eval(serverConfig.load[F])
            aConf <- Stream.eval(apiConfig.load[F]) 
            server <- ServerUtil.oauthServer(
                            sConf.hostValue,
                            sConf.portValue,
                            githubRoutes(
                                aConf.key,
                                aConf.secret.value
                            )
                        )
        yield server
    def run(args: List[String]): IO[ExitCode] = program[IO].compile.last.flatMap(_.get.use(_ => IO.never)).as(ExitCode.Success)
    