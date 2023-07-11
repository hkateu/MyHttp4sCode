package com.xonal.oAuth

import io.circe.Decoder
import cats.effect.IO
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.implicits.uri
import org.http4s.headers.{Accept,Authorization}
import org.http4s.*
import cats.effect.Async
import com.xonal.config.{Config, configuration}

object OauthImpl:
    final case class GithubResponse(accessToken: String, tokenType: String, scope: String)
    
    object GithubResponse:
        given decoder: Decoder[GithubResponse] = Decoder.instance{h =>
            for 
                access_token <- h.get[String]("access_token")
                token_type <- h.get[String]("token_type")
                scope <- h.get[String]("scope")
            yield GithubResponse(access_token,token_type,scope)    
        }

    def decodeJson[F](jsonString: StringBuffer) =
             decode[GithubResponse](str)

    def getJsonString[F[_]: Async](req: Request[F]): F[String] =
        EmberClientBuilder.default[F].build.use(client => client.expect[String](req))

    def fetchJsonString[F[_]: Async](code: String, config: Config = configuration.load) = {
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

    def fetchGithubDetails[F[_]: Async](access_token: String) = {
        val req = Request[F](
            Method.GET,
            uri"https://api.github.com/user/emails",
            headers = Headers(
                Accept(MediaType.application.json),
                Authorization(Credentials.Token(AuthScheme.Bearer, access_token)))
            )
        getJsonString(req)
    }

    def program(code: String) = 
        for 
            token <- fetchJsonString(code)
            response <- decodeJson(token)
            gitDetails <- fetchGithubDetails(response.accessToken)
        yield gitDetails
end OauthImpl