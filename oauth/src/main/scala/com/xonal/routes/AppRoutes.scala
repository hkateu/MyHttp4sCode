package com.xonal.routes

import org.http4s.dsl.impl.QueryParamDecoderMatcher
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import cats.Applicative
import com.xonal.oAuth.OauthImpl.{fetchJsonString,fetchGithubEmail,GithubResponse}
import io.circe.parser.*
import org.http4s.circe.jsonEncoderOf
import org.http4s.EntityEncoder
import cats.effect.Async
import cats.syntax.all.*
import org.http4s.StaticFile
import java.nio.file.Paths
import fs2.io.file.Path
import com.xonal.config.Config
import cats.implicits.*

object GithubRoutes:
    object GithubTokenQueryParamMatcher extends QueryParamDecoderMatcher[String]("code")

    def githubRoutes[F[_]: Async](config: Config): HttpRoutes[F] = {
        val dsl = Http4sDsl[F]
        import dsl.{Path as _, *}
        HttpRoutes.of[F]{
            case request @ GET -> Root / "index.html" =>
                StaticFile.fromPath(Path("oauth/src/main/scala/com/xonal/index.html"), Some(request))
                .getOrElseF(NotFound()) // In case the file doesn't exist
            case GET -> Root / "callback" :? GithubTokenQueryParamMatcher(code) => 
                fetchJsonString(code, config.api.key, config.api.secret.value).flatMap{token =>
                    decode[GithubResponse](token) match
                        case Right(payload) =>
                            Ok(fetchGithubEmail(payload.accessToken)) 
                        case Left(e) => 
                            println(e)
                            Ok(s"An error occurred: ${e.getMessage}")
                }
        }
    }