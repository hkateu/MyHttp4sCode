package com.xonal
import cats.effect.* 
import cats.syntax.all.* 
import org.http4s.* 
import org.http4s.dsl.io.* 
import org.http4s.implicits.*
import org.http4s.headers.`Cache-Control`
import org.http4s.CacheDirective.`no-cache`
import cats.data.NonEmptyList
import java.nio.charset.StandardCharsets.UTF_8
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import fs2.Stream
import scala.concurrent.duration._
import java.time.LocalDate
import scala.util.Try
import org.http4s.dsl.impl.MatrixVar
import java.time.Year
import java.time.Instant
import java.time.format.DateTimeFormatter
import cats.data.ValidatedNel

object Dsl extends IOApp:
    val service = HttpRoutes.of[IO] {
        case _ => 
            IO(Response(Status.Ok))
    }

    val getRoot = Request[IO](Method.GET, uri"/")
    val serviceIO = service.orNotFound.run(getRoot)

    //status code
    val okIo: IO[Response[IO]] = Ok() //200
    val service_204 = HttpRoutes.of[IO]{
        case _ => NoContent()
    }.orNotFound.run(getRoot)

    val program = 
        for
            sio <- serviceIO
            sio204 <- service_204 
        yield List(sio,sio204).foreach(println)
    //headers
    val header1 = Ok("Ok response.")
    val header2 = Ok("Ok response.", `Cache-Control`(NonEmptyList(`no-cache`(), Nil)))
    val header3 = Ok("Ok response.", "X-Auth-Token" -> "value")
    
    val program2 = 
        for 
            h1 <- header1
            h2 <- header2
            h3 <- header3
        yield List(h1,h2,h3).map(_.headers).foreach(println)

    //cookies
    val cookie1 = Ok("Ok response.").map(_.addCookie(ResponseCookie("foo","bar")).headers)
    val cookieResp = 
        for
            resp <- Ok("Ok response.")
            now <- HttpDate.current[IO]
        yield resp.addCookie(ResponseCookie(
            "foo",
            "bar",
            expires = Some(now), 
            httpOnly = true, 
            secure = true
            )).headers
    val cookie3 = Ok("Ok response.").map(_.removeCookie("foo").headers) 

    val program3 = 
        for 
            c1 <- cookie1
            c2 <- cookieResp
            c3 <- cookie3
        yield List(c1,c2,c3).foreach(println)

    //Responding with a body
    val resp1 = Ok("binary".getBytes(UTF_8))

    val ioFuture = Ok(IO.fromFuture(IO(Future{
        println("I run when the furture is constructed.")
        "Greetings from the future!"
    })))

    val io = Ok(IO{
        println("I run when the IO is run.")
        "Mission accomplished"
    })

    val program4 = 
    for
        r1 <- resp1
        r2 <- ioFuture
        r3 <- io 
    yield List(r1,r2,r3).map(_.headers).foreach(println)

    //streaming bodies
    val drip: Stream[IO,String] = 
        Stream
        .awakeEvery[IO](100.millis)
        .map(_.toString)
        .take(10)

    val dripOutIO = 
        drip
        .through(fs2.text.lines)
        .evalMap(s => {IO{println(s); s}})
        .compile
        .drain

    val okStream = Ok(drip).map(println)

    //matching and extracting requests
    // matches to /
    val basicResp = HttpRoutes.of[IO]{
        case GET -> Root => Ok("root")
    }

    //matches expected depth
    val expResp = HttpRoutes.of[IO]{
        case GET -> Root / "hello" / name =>
            Ok(s"Hello, ${name}!")
    }

    //matching arbitrary depth
    HttpRoutes.of[IO]{
        case GET -> "hello" /: rest => 
            Ok(s"""Hello, ${rest.segments.mkString(" and ")}!""")
    }

    //matching file types
    HttpRoutes.of[IO]{
        case GET -> Root / file ~ "json" =>
            Ok(s"""{"response": You asked for $file"}""")
    }


    //Handling Path Parameters
    def getUserName(user: Int): IO[String] = IO(s"UserId is: $user")
    val userService = HttpRoutes.of[IO]{
        case GET -> Root / "users" / IntVar(userId) =>
            Ok(getUserName(userId))
    }

    //custom extractor object
    object LocalDateVar:
        def unapply(str: String): Option[LocalDate] = 
            if !str.isEmpty then
                Try(LocalDate.parse(str)).toOption
            else
                None

    def getTempretureForecast(date: LocalDate): IO[Double] = IO(42.23)
    val dailyWeatherService = HttpRoutes.of[IO]{
        case GET -> Root / "weather" / "temperature" / LocalDateVar(localDate) => 
            Ok(getTempretureForecast(localDate).map(s"The temperature on $localDate will be: " + _))
    }

    val request = Request[IO](Method.GET, uri"/weather/temperature/2016-11-05")
    val weatherResp = dailyWeatherService.orNotFound(request).map(println)

    //Handling Matrix Path Parameters
    object FullNameExtractor extends MatrixVar("name", List("first","last"))

    val greetingService = HttpRoutes.of[IO]{
        case GET -> Root / "hello" / FullNameExtractor(first,last) / "greeting" =>
            Ok(s"Hello, $first $last.")
    }

    val greetingResp = greetingService
        .orNotFound(Request[IO](
            method = Method.GET,
            uri = uri"/hello/name;first=john;last=doe/greeting"
        )).map(println)

    object FullNameAndIDExtractor extends MatrixVar("name",List("first","last","id"))

    val greetingWithIdService = HttpRoutes.of[IO]{
        case GET -> Root / "hello" / FullNameAndIDExtractor(first,last,IntVar(id)) / "greeting" => 
            Ok(s"Hello, $first $last. Your User ID is $id.")
    }

    val greetingWithIdResp = greetingWithIdService
        .orNotFound(Request[IO](
            method = Method.GET,
            uri = uri"/hello/name;first=john;last=doe;id=123/greeting"
        )).map(println)

    object CountryQueryParamMatcher extends QueryParamDecoderMatcher[String]("country")

    //given yearQueryParamDecoder: QueryParamDecoder[Year] = QueryParamDecoder[Int].map(Year.of)

    object YearQueryParamMatcher extends QueryParamDecoderMatcher[Year]("year")

    def getAverageTemperatureForCountryAndYear(country: String, year: Year): IO[Double] = ???

    val getAverageTemperatureService = HttpRoutes.of[IO]{
        case GET -> Root / "weather" / "temperature" :? CountryQueryParamMatcher(country) +& YearQueryParamMatcher(year) => 
            Ok(getAverageTemperatureForCountryAndYear(country,year).map(s"Average temperature for $country in $year was: " + _))
    }

    //using query param codecs
    given isoInstantCodec: QueryParamCodec[Instant] = 
        QueryParamCodec.instantQueryParamCodec(DateTimeFormatter.ISO_INSTANT)

    object IsoInstantParamMatcher extends QueryParamDecoderMatcher[Instant]("timestamp")

    //optional query parameters
    //given year param decoder in scope

    object OptionalYearQueryParamMatcher extends OptionalQueryParamDecoderMatcher[Year]("year")

    def getAverageTemperatureForCurrentYear: IO[String] = IO("25")
    def getAverageTemperatureForYear(y: Year): IO[String] = IO("50")

    val routes = HttpRoutes.of[IO]{
        case GET -> Root / "temperature" :? OptionalYearQueryParamMatcher(maybeYear) =>
            maybeYear match
                case None => Ok(getAverageTemperatureForCurrentYear)
                case Some(year) => Ok(getAverageTemperatureForYear(year)) 
    }

    //invalid Query Parameter Handling
    given yearQueryParamDecoder: QueryParamDecoder[Year] = 
        QueryParamDecoder[Int]
        .emap(i => Try(Year.of(i))
        .toEither
        .leftMap(t => ParseFailure(t.getMessage, t.getMessage)))

    object YearQueryParamMatcher2 extends ValidatingQueryParamDecoderMatcher[Year]("year")

    val routes2 = HttpRoutes.of[IO]{
        case GET -> Root / "temperature" :? YearQueryParamMatcher2(yearValidated) => 
            yearValidated.fold(
                parseFailures => BadRequest("unable to parse argument year"),
                year => Ok(getAverageTemperatureForYear(year))
            )
    }

    //optional validated param matchers
    object LongParamMatcher extends OptionalValidatingQueryParamDecoderMatcher[Long]("long")

    val routes3 = HttpRoutes.of[IO]{
        case GET -> Root / "number" :? LongParamMatcher(maybeNumber) => 
            //val _: Option[ValidatedNel[ParseFailure,Long]] = maybeNumber
            maybeNumber match
                case Some(n) =>
                    n.fold(
                        parseFailures => BadRequest("unable to parse argument 'long'"),
                        year => Ok(n.toString)
                    )
                case None => BadRequest("missing number") 
    }

    override def run(args: List[String]): IO[ExitCode] = program3.as(ExitCode.Success)

