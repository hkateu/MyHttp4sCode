package com.xonal

import cats.effect.*
import io.circe.* 
// import io.circe.literal.* 
import org.http4s.* 
import org.http4s.dsl.io.* 
import org.http4s.implicits.*
import io.circe.syntax.*
import io.circe.generic.auto.*
import org.http4s.circe.*
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.ember.client._
import org.http4s.client.Client

object JsonHandling extends IOApp:
    // def hello(name: String): Json = 
    //     json"""{"hello: $name"}"""

    // val greeting = hello("workd")

    // Request[IO](Method.Post, uri"/hello").withEntity(json"""{"name":"Alice"}""")

    case class Hello(name: String)
    case class User(name: String)

    // given HelloEncoder: Encoder[Hello] = 
    //     Encoder.instance{(hello: Hello) =>
    //         json"""{"hello": ${hello.name}}"""    
    //     }

    Hello("Alice").asJson

    Request[IO](Method.POST, uri"/hello").withEntity(User("Bob").asJson)

    //receiving json
    Ok("""{"name":"Alice"}""").flatMap(_.as[Json])

    Request[IO](Method.POST, uri"/hello")
        .withEntity("""{"name":"Bob"}""")
        .as[Json]

    //Decoding Json
    given userDecoder: EntityDecoder[IO,User] = jsonOf[IO,User]

    Ok("""{"name":"Bob"}""").flatMap(_.as[User])

    Request[IO](Method.POST, uri"/hello")
        .withEntity("""{"name":"Bob"}""")
        .as[User]

    val jsonApp = HttpRoutes.of[IO]{
        case req @ POST -> Root / "hello" =>
            for{
                user <- req.as[User]
                resp <- Ok(Hello(user.name).asJson)
            } yield (resp)
    }.orNotFound

    val req = Request[IO](Method.POST, uri"http://localhost:8080/hello")
                .withEntity(User("Herbert").asJson)

    val client = Client.fromHttpApp[IO](jsonApp).expect(req)(jsonOf[IO,Hello])
    def run(args: List[String]): IO[ExitCode] = client.map(x => println(x.name)).as(ExitCode.Success)