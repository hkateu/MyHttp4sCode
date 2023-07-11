package com.xonal

import cats.effect.{IO, IOApp, ExitCode, MonadCancelThrow}
import org.http4s.* 
import org.http4s.dsl.io.*
import org.http4s.implicits.* 
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.middleware.Logger
import com.comcast.ip4s.*
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.client.JavaNetClientBuilder
import cats.syntax.all.*
import org.typelevel.ci.*
import org.http4s.headers.Authorization
import org.http4s.headers.Accept
import org.http4s.circe.* 
import io.circe.generic.auto.*
import cats.effect.kernel.Resource

object Http4sClient extends IOApp:
    case class AuthResponse(grant_type: String, client_id: String, client_secret: String)

    given decoder: EntityDecoder[IO, AuthResponse] = jsonOf[IO, AuthResponse]

    val app = HttpRoutes.of[IO]{
        case GET -> Root / "hello" / name =>
            Ok(s"Hello, $name")
        case req @ POST -> Root / "my-lovely-api" / "token" => 
            Ok(req.as[AuthResponse].map(x => x.toString()))
    }.orNotFound

    val finalHttpApp = Logger.httpApp(true,true)(app)

    def hello(name: String) = {
        Request[IO](method = Method.GET, uri = uri"/hello/" / name)
    }

    def printHello(app: HttpApp[IO], req: Request[IO]): IO[Unit] = 
        Client.fromHttpApp[IO](app).expect[String](req).flatMap(IO.println)

    val inputs = List("Ember", "http4s", "Scala")

    val client = EmberClientBuilder
            .default[IO]
            .build
            .use(_ => inputs.parTraverse(x => printHello(finalHttpApp, hello(x))))

    //simple client middleware
    def addTestHeader[F[_]: MonadCancelThrow](underlying: Client[F]): Client[F] = 
        Client[F]{req =>
            underlying
                .run(
                    req.withHeaders(Header.Raw(ci"X-Test-Request", "test"))
                )
                .map(
                    _.withHeaders(Header.Raw(ci"X-Test-Response", "test"))
                )    
        }

    val request = Request[IO](
        method = Method.GET,
        uri = uri"https://my-lovely-api.com/",
        headers = Headers(
            Authorization(Credentials.Token(AuthScheme.Bearer, "open sesame")),
            Accept(MediaType.application.json)
        )
    )



    val postRequest = Request[IO](
        method = Method.POST,
        uri = uri"/my-lovely-api/token"
    ).withEntity(
        UrlForm(
            "grant_type" -> "client_credentials",
            "client_id" -> "my-awesome-client",
            "client_secret" -> "s3cr3t"
        )
    )


    // def printHello2(app: HttpApp[IO], req: Request[IO]): IO[Unit] = 
    //     Client.fromHttpApp[IO](app).expect[String](req).flatMap(IO.println)

    //calling to a json API
    val endpoint = uri"http://localhost:8080/hello/Ember"

    val myClient = Client.fromHttpApp[IO](app).get[Either[String, String]](endpoint) {
        case Status.Successful(r) => r.attemptAs[String].leftMap(_.message).value 
        case r => r.as[String].map(b => Left(s"Request failed with status ${r.status.code} and body $b"))
    }.map{value => 
        value match
            case Right(v) => v
            case Left(v) => v        
    }
    val client2 = EmberClientBuilder
            .default[IO]
            .build
            .use(_ => myClient.map(println))


    def run(args: List[String]): IO[ExitCode] = client2.as(ExitCode.Success)