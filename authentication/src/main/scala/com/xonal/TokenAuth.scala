package com.xonal

import cats.effect.{IOApp, IO, ExitCode}
import org.http4s.* 
import org.http4s.dsl.io.* 
import org.http4s.ember.server.*
import com.comcast.ip4s.*
import cats.implicits.*
import dev.profunktor.auth.*
import dev.profunktor.auth.jwt.*
import pdi.jwt.*
import java.time.Instant
import io.circe.* 
import io.circe.parser.*
// import scala.util.Try
// import scala.util.Success
// import scala.util.Failure
// import org.http4s.server.*  
// import org.http4s.implicits.* 
// import cats.Applicative
// import pdi.jwt.algorithms.JwtHmacAlgorithm
object TokenAuth extends IOApp:
    case class AuthUser(id: Long, name: String)
    case class TokenPayLoad(user: String, level: String)

    object TokenPayLoad:
        given decoder: Decoder[TokenPayLoad] = Decoder.instance{h =>
            for 
                user <- h.get[String]("user")
                level <- h.get[String]("level")
            yield TokenPayLoad(user,level)    
        }

    val claim = JwtClaim(content = """{"user":"John", "level":"basic"}""",expiration = Some(Instant.now.plusSeconds(157784760).getEpochSecond),
    issuedAt = Some(Instant.now.getEpochSecond))

    val key = "secretKey"

    val algo = JwtAlgorithm.HS256

    val token = JwtCirce.encode(claim, key, algo)

    val database = Map("John" -> AuthUser(123,"JohnDoe"))

    val authenticate: JwtToken => JwtClaim => IO[Option[AuthUser]] =
        (token: JwtToken) =>
            (claim: JwtClaim)
            => decode[TokenPayLoad](claim.content) match
                    case Right(payload) =>
                        IO(database.get(payload.user))
                    case Left(_) => IO(None)
                    
    val jwtAuth = JwtAuth.hmac(key, algo)
    val middleware = JwtAuthMiddleware[IO, AuthUser](jwtAuth, authenticate)

    val authedRoutes: AuthedRoutes[AuthUser,IO] = 
    AuthedRoutes.of{
        case GET -> Root / "welcome" as user =>
            Ok(s"Welcome, ${user.name}")
    }

    val loginRoutes: HttpRoutes[IO] = 
        HttpRoutes.of[IO]{
            case GET -> Root / "login" => 
                Ok(s"Logged In").map(_.addCookie(ResponseCookie("token", token)))
        }

    val securedRoutes: HttpRoutes[IO] = middleware(authedRoutes)

    val service = loginRoutes <+> securedRoutes

    val server = EmberServerBuilder
    .default[IO]
    .withHost(ipv4"0.0.0.0")
    .withPort(port"8080")
    .withHttpApp(service.orNotFound)
    .build

    override def run(args: List[String]): IO[ExitCode] = server.use(_ => IO.never).as(ExitCode.Success)