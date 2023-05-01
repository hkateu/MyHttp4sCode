package com.xonal

import cats.effect.*
import org.http4s.*
import org.http4s.dsl.io.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.http4s.circe.* 
import org.http4s.server.Router
import org.http4s.ember.server.*
import com.comcast.ip4s.*
import cats.syntax.all.*

object service extends IOApp:

  val helloworldservice = HttpRoutes.of[IO]{
    case GET -> Root / "hello" / name =>
      Ok(s"Hello, $name")
  }

  case class Tweet(message:String)

  val myTweets = Map(1 -> Tweet("First Tweet"), 2 -> Tweet("Second Tweet"))

  def getTweet(tweetId: Int): IO[Option[Tweet]] = IO(myTweets.get(tweetId))

  def getPopularTweets(): IO[Seq[Tweet]] = IO(myTweets.values.toSeq)
  object TweetQueryParamMatcher extends QueryParamDecoderMatcher[Int]("tweetId")

  given tweetDecoder: EntityDecoder[IO,Tweet] = jsonOf[IO,Tweet]

  val tweetService = HttpRoutes.of[IO]{
    case GET -> Root / "tweets" / "popular" =>
      getPopularTweets().flatMap(popTweets => Ok(popTweets.asJson))
    case GET -> Root / "tweets" / IntVar(tweetId) =>
      getTweet(tweetId).flatMap{id => id match
        case Some(value) => Ok(value.asJson)
        case None => BadRequest("value not found")
      }
  }

  val services = tweetService <+> helloworldservice
  val httpApp = Router(
    "/helloService" -> helloworldservice,
    "/tweetService" -> tweetService,
    "/all" -> services
  ).orNotFound

  val server = EmberServerBuilder
    .default[IO]
    .withHost(ipv4"0.0.0.0")
    .withPort(port"8080")
    .withHttpApp(httpApp)
    .build

  override def run(args: List[String]): IO[ExitCode] = server.use(_ => IO.never).as(ExitCode.Success)
