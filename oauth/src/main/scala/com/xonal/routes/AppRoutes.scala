package com.xonal.routes

import org.http4s.dsl.impl.QueryParamDecoderMatcher
import org.http4s.*
import org.http4s.dsl.Http4sDsl
import com.xonal.oAuth.OauthImpl.getOauthResults
import cats.effect.Async
import fs2.io.file.Path
import cats.syntax.all.*

object GithubRoutes {
  object GithubTokenQueryParamMatcher
      extends QueryParamDecoderMatcher[String]("code")

  def githubRoutes[F[_]: Async]: HttpRoutes[F] = {
    val dsl = Http4sDsl[F]
    import dsl.{Path as _, *}
    HttpRoutes.of[F] {
      case request @ GET -> Root / "index.html" =>
        StaticFile
          .fromPath(
            Path("oauth/src/main/scala/com/xonal/index.html"),
            Some(request)
          )
          .getOrElseF(NotFound()) // In case the file doesn't exist
      case GET -> Root / "callback" :? GithubTokenQueryParamMatcher(code) =>
        getOauthResults(code).handleError(_.getMessage).flatMap(Ok(_))
    }
  }
}
