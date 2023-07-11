package com.xonal 

import cats.effect.* 
import scala.concurrent.duration.* 
import fs2.Stream
import org.http4s.* 
import org.http4s.dsl.io.* 
import org.http4s.ember.client.*
import org.http4s.client.oauth1
import org.http4s.client.oauth1.ProtocolParameter.* 
import org.http4s.implicits.* 
import fs2.io.stdout
import fs2.text.{lines,utf8}
import io.circe.Json
import org.typelevel.jawn.fs2.* 
import org.typelevel.jawn.Facade

object Streaming extends IOApp:
    val seconds = Stream.awakeEvery[IO](1.second)

    val routes = HttpRoutes.of[IO]{
        case GET -> Root / "seconds" => 
            Ok(seconds.map(_.toString))
    }

    class TWStream[F[_]: Async] {
        given f: Facade[Json] = new io.circe.jawn.CirceSupportParser(None, false).facade
        
        def sign(consumerKey: String, 
                consumerSecret: String, 
                accessToken: String, 
                accessSecret: String)
                (req: Request[F]): F[Request[F]] = {
                    val consumer = Consumer(consumerKey, consumerSecret)
                    val token = Token(accessToken, accessSecret)
                    oauth1.signRequest(req, consumer, Some(token), realm = None, timestampGenerator = Timestamp.now, nonceGenerator = Nonce.now)
        }

        def jsonStream(consumerKey: String, 
                    consumerSecret: String, 
                    accessToken: String, 
                    accessSecret: String)
                    (req: Request[F]): Stream[F,Json] = 
                        for 
                            client <- Stream.resource(EmberClientBuilder.default[F].build) 
                            sr <- Stream.eval(sign(consumerKey, consumerSecret, accessToken, accessSecret)(req))
                            res <- client.stream(sr).flatMap(_.body.chunks.parseJsonStream)
                        yield res

        val stream: Stream[F, Unit] = {
            val req = Request[F](Method.GET, uri"https://stream.twitter.com/1.1/statuses/sample.json")
            val s = jsonStream("<consumerkey>", "<consumerSecret>", "<accessToken>", "<accessSecret>")(req)
            s.map(_.spaces2).through(lines).through(utf8.encode).through(stdout)
        }

        def run: F[Unit] = stream.compile.drain
    }

//f49e598e44ee5a7c8f58 clientId
//3eff0b277d1823fa5edcc71c690b1cb699716337 secret key
//https://github.com/login/oauth/authorize?scope=user:email&client_id=<%= client_id %>
    case class Otpurl(url: String)
    def run(args: List[String]): IO[ExitCode] = (new TWStream[IO]).run.as(ExitCode.Success)