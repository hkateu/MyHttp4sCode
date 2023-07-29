package com.xonal.oAuth

import io.circe.{Decoder, Error}
import io.circe.parser.decode
import org.http4s.ember.client.EmberClientBuilder
import cats.effect.Async
import org.http4s.*
import com.xonal.config.{Config, configuration}
import org.http4s.implicits.uri
import org.http4s.headers.{Accept, Authorization}
import cats.syntax.all.*

object OauthImpl {
  final case class GithubResponse(
      accessToken: String,
      tokenType: String,
      scope: String
  )

  private object GithubResponse{
    given decoder: Decoder[GithubResponse] = Decoder.instance { h =>
      for
        access_token <- h.get[String]("access_token")
        token_type <- h.get[String]("token_type")
        scope <- h.get[String]("scope")
      yield GithubResponse(access_token, token_type, scope)
    }
  }
    
  private def decodeJson(jsonString: String): Either[Error, GithubResponse] =
    decode[GithubResponse](jsonString)

  private def getJsonString[F[_]: Async](req: Request[F]): F[String] =
    EmberClientBuilder
      .default[F]
      .build
      .use(client => client.expect[String](req))

  private def fetchJsonString[F[_]: Async](
      code: String,
      config: Config
  ): F[String] = {
    val form = UrlForm(
      "client_id" -> config.api.key,
      "client_secret" -> config.api.secret.value,
      "code" -> code
    )

    val req = Request[F](
      Method.POST,
      uri"https://github.com/login/oauth/access_token",
      headers = Headers(Accept(MediaType.application.json))
    ).withEntity(form)

    getJsonString(req)
  }

  private def fetchGithubDetails[F[_]: Async](
      access_token: String
  ): F[String] = {
    val req = Request[F](
      Method.GET,
      uri"https://api.github.com/user/emails",
      headers = Headers(
        Accept(MediaType.application.json),
        Authorization(Credentials.Token(AuthScheme.Bearer, access_token))
      )
    )
    getJsonString(req)
  }

  def getOauthResults[F[_]: Async](code: String): F[String] =
    for
      config <- configuration.load[F]
      decodedJson <- fetchJsonString[F](code, config).map(decodeJson(_))
      githubDetails <- decodedJson match
        case Right(v)  => fetchGithubDetails(v.accessToken)
        case Left(err) => err.pure[F].map(_.getMessage)
    yield githubDetails

}
