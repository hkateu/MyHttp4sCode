package com.xonal

import cats.effect.* 
import cats.syntax.all._
import io.circe.* 
import io.circe.syntax.*
import io.circe.generic.semiauto.* 
import org.http4s.* 
import org.http4s.circe.* 
import org.http4s.dsl.io.* 
import org.http4s.implicits.*
import org.http4s.client.Client

object Testing extends IOApp:
    case class User(name: String, age: Int)
    given UserEncoder: Encoder[User] = deriveEncoder[User]

    trait UserRepo[F[_]]:
        def find(userId: String): F[Option[User]]

    def httpRoutes[F[_]](repo: UserRepo[F])(using F: Async[F]): HttpRoutes[F] = 
        HttpRoutes.of[F]{
            case GET -> Root / "user" / id =>
                repo.find(id).map{
                    case Some(user) => Response(status = Status.Ok).withEntity(user.asJson)
                    case None => Response(status = Status.NotFound) 
                }
        }


    //return true if match succeeds, otherwise false

    def check[A](actual: IO[Response[IO]], 
                expectedStatus: Status, 
                expectedBody: Option[A])
                (using ev: EntityDecoder[IO,A]): IO[Boolean] = {
                    // val actualResp = actual
                    val statusCheck = actual.map(actualresp => actualresp.status == expectedStatus)
                    val bodyCheck = expectedBody.fold[IO[Boolean]](
                        for 
                            actualResp <- actual
                            vec <- actualResp.body.compile.toVector
                        yield vec.isEmpty
                    )(
                        expected =>
                            for
                                actualResp <- actual
                                res <- actualResp.as[A]
                            yield res == expected
                    )

                    val finalCheck = for 
                        sc <- statusCheck
                        bc <- bodyCheck
                    yield sc && bc

                    finalCheck
    }

    val success: UserRepo[IO] = new UserRepo[IO]{
        def find(id: String): IO[Option[User]] = IO.pure(Some(User("johndoe", 42)))
    }

    val response: IO[Response[IO]] = httpRoutes[IO](success).orNotFound.run(
        Request(method = Method.GET, uri = uri"/user/not-used")
    )

    val expectedJson = Json.obj(
        "name" := "johndoe",
        "age" := 42
    )

    val chk = check[Json](response, Status.Ok, Some(expectedJson))

    val foundNone: UserRepo[IO] = new UserRepo{
        def find(id: String): IO[Option[User]] = IO.pure(None)
    }

    val respFoundNone: IO[Response[IO]] = 
        httpRoutes[IO](foundNone).orNotFound.run(
            Request(method = Method.GET, uri = uri"/user/not-used")
        )

    val chk2 = check[Json](respFoundNone, Status.NotFound, None) //true

    val doesNotMatter: UserRepo[IO] = new UserRepo[IO]{
        def find(id: String): IO[Option[User]] = IO.raiseError(new RuntimeException("Should not get caled!"))
    }

    val respNotFound: IO[Response[IO]] = 
        httpRoutes[IO](doesNotMatter).orNotFound.run(
            Request(method = Method.GET, uri"/not-a-matching-path")
        )

    val chk3 = check[String](respNotFound, Status.NotFound, Some("Not found")) // true

    //using a client

    val request: Request[IO] = Request(method = Method.GET, uri"user/not-used")
    val client: Client[IO] = Client.fromHttpApp(httpRoutes[IO](doesNotMatter).orNotFound)
    val resp: IO[Json] = client.expect[Json](request)

    val asrt = resp.map(r => r == expectedJson)

    def run(args: List[String]): IO[ExitCode] = chk3.map(println).as(ExitCode.Success)