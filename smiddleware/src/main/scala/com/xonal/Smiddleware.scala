package com.xonal

import cats.effect.*
import cats.syntax.all.* 
import org.typelevel.ci.* 
import org.http4s.* 
import org.http4s.dsl.io.* 
import org.http4s.implicits.* 
import org.http4s.client.Client
import scala.concurrent.duration.* 
import cats.effect.std.Random
import fs2.Stream
import cats.effect.std.Console
import org.http4s.server.middleware.* 
import org.http4s.server.middleware.HttpMethodOverrider.{HttpMethodOverriderConfig, QueryOverrideStrategy}
import cats.data.*
import org.http4s.server.{ContextMiddleware, HttpMiddleware}
import org.http4s.metrics.{MetricsOps, TerminationType}

object Smiddleware extends IOApp:
    object NameQueryParameterMatcher extends QueryParamDecoderMatcher[String]("name")

    val service = HttpRoutes.of[IO]{
        case GET -> Root / "bad" => 
            BadRequest()
        case GET -> Root / "ok" =>
            Ok()
        case r @ POST -> Root / "post" => 
            r.as[Unit] >> Ok()
        case r @ POST -> Root / "echo" =>
            r.as[String].flatMap(Ok(_))
        case GET -> Root / "b" / "c" =>
            Ok()
        case POST -> Root / "queryForm" :? NameQueryParameterMatcher(name) =>
            Ok(s"hello $name")
        case GET -> Root / "wait" =>
            IO.sleep(10.millis) >> Ok()
        case GET -> Root / "boom" => 
            IO.raiseError(new RuntimeException("boom!"))
        case r @ POST -> Root / "reverse" =>
            r.as[String].flatMap(s => Ok(s.reverse))
        case GET -> Root / "forever" => 
            IO(Response[IO](headers = Headers("hello" -> "hi")).withEntity(Stream.constant("a").covary[IO]))
        case r @ GET -> Root / "doubleRead" =>
            val myStr = 
                for
                    str <- r.as[String]
                yield s"$str == $str"
            myStr.flatMap(Ok(_))
        case GET -> Root / "random" =>
            Random.scalaUtilRandom[IO].flatMap(_.nextInt).flatMap(random => Ok(random.toString))
    }

    val okRequest = Request[IO](Method.GET, uri"/ok")
    val badRequest = Request[IO](Method.GET, uri"/bad")
    val postRequest = Request[IO](Method.POST, uri"/post")
    val waitRequest = Request[IO](Method.GET, uri"/wait")
    val boomRequest = Request[IO](Method.GET, uri"/boom")
    val reverseRequest = Request[IO](Method.GET, uri"/reverse")

    val client = Client.fromHttpApp(service.orNotFound)

    //Headers
    //Cache tells client how to cache response
    val cacheService = Caching.cache(
        3.hours,
        isPublic = Left(CacheDirective.public),
        methodToSetOn = _ == Method.GET,
        statusToSetOn = _.isSuccess,
        service
    ).orNotFound

    val cacheClient = Client.fromHttpApp(cacheService)
    // cacheClient.run(okRequest).use(_.headers.pure[IO])
    // cacheClient.run(badRequest).use(_.headers.pure[IO])
    // cacheClient.run(postRequest).use(_.headers.pure[IO])

    //Date
    //Adds current date to response
    val dateService = Date.httpRoutes(service).orNotFound
    val dateClient = Client.fromHttpApp(dateService)
    // dateClient.run(okRequest).user(_.headers.pure[IO])

    //HeaderEcho
    //Adds headers included in the request
    val echoService = HeaderEcho.httpRoutes(echoHeadersWhen = _ => true)(service).orNotFound
    val echoClient = Client.fromHttpApp(echoService)
    // echoClient.run(okRequest.putHeaders("Hello" -> "hi")).use(_.headers.pure[IO])

    //ResponseTiming
    //sets response header with the request duration
    val timingService = ResponseTiming(service.orNotFound)
    val timingClient = Client.fromHttpApp(timingService)
    // timingClient.run(okRequest).user(_.headers.pure[IO])

    //Request Id
    //Generates an X-Request-ID header
    val requestIdService = RequestId.httpRoutes(HttpRoutes.of[IO]{
        case req => 
            val reqId = req.headers.get(ci"X-Request-ID").fold(null)(_.head.value)
            //use the request id to correlate logs with the request
            Console[IO].println(s"request recieved, cid=$reqId") *> Ok()
    })
    val requestIdClient = Client.fromHttpApp(requestIdService.orNotFound)
    // requestIdClient.run(okRequest).use(resp =>
    //     (resp.headers, resp.attributes.lookup(RequestId.requestIdAttrKey)).pure[IO])

    //StaticHeaders
    //Add static headers to response
    val staticHeadersService = StaticHeaders(Headers("X-Hello" -> "hi"))(service).orNotFound
    val staticHeaderClient = Client.fromHttpApp(staticHeadersService)
    // staticHeaderClient.run(okRequest).use(_.headers.pure[IO])

    //Request rewrititng
    //AutoSlash
    //removes trailing slash from the requested url
    val autoSlashService = AutoSlash(service).orNotFound
    val autoSlashClient = Client.fromHttpApp(autoSlashService)
    val okWithSlash = Request[IO](Method.GET, uri"/ok/")
    // client.status(okRequest)
    // client.status(okWithSlash)
    // autoSlashClient.status(okRequest)
    // autoSlashClient.status(okWithSlash)

    //DefaultHead
    //Provides a native implementation of HEAD request for any GET routes
    val headService = DefaultHead(service).orNotFound
    val headClient = Client.fromHttpApp(headService)
    // headClient.status(Request[IO](Method.HEAD, uri"/forever"))
    // headClient.run(Request[IO](Method.HEAD, uri"/forever")).use(_.headers.pure[IO])
    
    //HttpMethodOverrider
    //allows client to disguuse the http verb of a request
    val overrideService = HttpMethodOverrider(
        service,
        HttpMethodOverriderConfig(
            QueryOverrideStrategy(paramName = "realMethod"),
            Set(Method.GET)
        )
    ).orNotFound
    val overrideClient = Client.fromHttpApp(overrideService)
    val overrideRequest = Request[IO](Method.GET, uri"/post?realMethod=POST")
    // client.status(overrideRequest)
    // overrideClient.status(overrideRequest)

    //HttpsRedirect
    //Redirects requests to https when the X-Forwarded-Proto header is http
    val httpsRedirectService = HttpsRedirect(service).orNotFound
    val httpsRedirectClient = Client.fromHttpApp(httpsRedirectService)
    val httpRequest = okRequest
        .putHeaders("Host" -> "example.com", "X-Forwarded-Proto" -> "http")
    // httpsRedirectClient.run(httpRequest).use(r => (r.headers, r.status).pure[IO])

    //TranslateUri
    //removes prefix from the path of the request url
    val translateService = TranslateUri(prefix = "a")(service).orNotFound
    val transalteRequest = Request[IO](Method.GET, uri"a/b/c")
    val translateClient = Client.fromHttpApp(translateService)
    // translateClient.status

  
    //UrlFormLifter
    //Transfroms x-www-form-urlencoded parameters into query parameter
    val urlFormService =   ???
        // UrlFormLifter[F[_]:Sync[IO], G[_]:Concurrent[IO]](G ~> F)(service.orNotFound)
    // val urlFormClient = Client.fromHttpApp(urlFormService)
    // val formRequest = Request[IO](Method.POST, uri"queryForm")
    //     .withEntity(UrlForm.single("name","John"))
    // urlFormClient.expect[String](formRequest)

    //Scaling and resource management

    // ConcurrentRequests
    //React to requests being accepted and completed, could be used for metrics
    def dropContext[A](middleware: ContextMiddleware[IO,A]): HttpMiddleware[IO] = 
        routes => middleware(Kleisli((c:ContextRequest[IO, A]) => routes(c.req)))

    val concurrentService = ConcurrentRequests.route[IO](
        onIncrement = total => Console[IO].println(s"someone comes to town, total=$total"),
        onDecrement = total => Console[IO].println(s"someone leaves town, total=$total")
    ).map((middle: ContextMiddleware[IO,Long]) => dropContext(middle)(service).orNotFound)

    val concurrentClient = concurrentService.map(Client.fromHttpApp[IO])
    concurrentClient.flatMap(cl => List.fill(3)(waitRequest).parTraverse(req => cl.expect[Unit](req))).void

    //EntityLimiter
    val limiterService = EntityLimiter.httpApp(service.orNotFound, limit = 16)
    val limiterClient = Client.fromHttpApp(limiterService)
    val smallRequest = postRequest.withEntity("*" * 15)
    val bigRequest = postRequest.withEntity("*" * 16)
    // limiterClient.status(smallRequest)
    // limiterClient.status(bigRequest)

    //MaxActiveRequests
    val maxService = MaxActiveRequests.forHttpApp[IO](maxActive = 2)
        .map(middleware => middleware(service.orNotFound))

    val maxClient = maxService.map(Client.fromHttpApp[IO])
    // maxClient.flatMap(cl => List.fill(5)(waitRequest).parTraverse(req => cl.status(req)))

    //Throttle
    val throttleService = Throttle.httpApp[IO](
        amount = 1,
        per = 10.millis
    )(service.orNotFound)
    val throttleClient = throttleService.map(Client.fromHttpApp[IO])
    // throttleClient.flatMap(cl => List.fill(5)(okRequest).traverse(req => IO.sleep(5.millis) >> cl.status(req)))

    //Timeout
    val timeoutService = Timeout.httpApp[IO](timeout = 5.milliseconds)(service.orNotFound)
    val timeoutClient = Client.fromHttpApp(timeoutService)
    // timeoutClient.status(waitRequest).timed

    //Error handling and Logging
    //ErrorAction
    val errorActionService = ErrorAction.httpRoutes[IO](
        service,
        (req, thr) => Console[IO].println("Oops: " + thr.getMessage)
    ).orNotFound

    val errorActionClient = Client.fromHttpApp(errorActionService)
    errorActionClient.expect[Unit](boomRequest).attempt

    //ErrorHandling
    val errorHandlingService = ErrorHandling.httpRoutes[IO](service).orNotFound
    val errorHandlingClient = Client.fromHttpApp(errorHandlingService)
    // client.status(boomRequest).attempt
    // errorHandlingClient.status(boomRequest)

    //Metrics
    val metricsOps = new MetricsOps[IO] {
        def increaseActiveRequests(classifier: Option[String]): IO[Unit] =
            Console[IO].println("increaseActiveRequests")
        def decreaseActiveRequests(classifier: Option[String]): IO[Unit] =
            IO.unit
        def recordHeadersTime(method: Method, elapsed: Long, classifier: Option[String]): IO[Unit] =
            IO.unit
        def recordTotalTime(method: Method, status: Status, elapsed: Long, classifier: Option[String]): IO[Unit] =
            IO.unit
        def recordAbnormalTermination(elapsed: Long, terminationType: TerminationType, classifier: Option[String]): IO[Unit] =
            Console[IO].println(s"abnormalTermination - $terminationType")
    }

    val metricsService = Metrics[IO](metricsOps)(service).orNotFound
    val metricsClient = Client.fromHttpApp(metricsService)
    // metricsClient.expect[Unit](boomRequest).attempt.void
    // metricsClient.expect[Unit](okRequest)

    //RequestLogger, ResponseLogger
    //Logger
    val loggerService = Logger.httpRoutes[IO](
        logHeaders = false,
        logBody = true,
        redactHeadersWhen = _ => false,
        logAction = Some((msg: String) => Console[IO].println(msg))
    )(service).orNotFound

    val loggerClient = Client.fromHttpApp(loggerService)
    // loggerClient.expect[Unit](reverseRequest.withEntity("mood"))

    //Advanced
    //BodyCache
    val bodyCacheService = BodyCache.httpRoutes(service).orNotFound
    val randomRequest = Request[IO](Method.GET, uri"/doubleRead")
        .withEntity(
            Stream.eval(
                Random.scalaUtilRandom[IO].flatMap(_.nextInt).map(random => random.toString)
            )
        )
    val bodyCacheClient = Client.fromHttpApp(bodyCacheService)
    // client.expect[String](randomRequest)
    // bodyCacheClient.expect[String](randomRequest)

    //BracketRequestResponse
    val ref = Ref[IO].of(0)
    val bracketMiddleware = BracketRequestResponse.bracketRequestResponseRoutes[IO,IO[Int]](
        acquire = ref.map(_.updateAndGet(_ + 1)))
        (release = _ => ref.map(_.update(_ - 1)))

    val bracketService = bracketMiddleware(ContextRoutes.of[IO[Int],IO]{
        case GET -> Root / "ok" as n => 
            n.flatMap(v => Ok(v.toString))
    }).orNotFound
    val bracketClient = Client.fromHttpApp(bracketService)
    // bracketClient.expect[String](okRequest)
    // ref.map(_.get)

    //ChunkAggregator
    def doubleBodyMiddleware(service: HttpRoutes[IO]): HttpRoutes[IO] = Kleisli{
        (req: Request[IO]) =>
            service(req).map{
                case Status.Successful(resp) => 
                    resp.withBodyStream(resp.body ++ resp.body)
                case resp => resp
            }
    }
    val chuckAggregatorService = doubleBodyMiddleware(ChunkAggregator.httpRoutes(service)).orNotFound
    val ChunkAggregatorClient = Client.fromHttpApp(chuckAggregatorService)
    // ChunkAggregatorClient.expect[String](Request[IO](Method.POST, uri"/echo").withEntity("foo"))
    //     .map(e => s"$e == foofoo")

    //ContextMiddleware
    case class UserId(raw: String)
    given userIdHeader: Header[UserId, Header.Single] = 
        Header.createRendered(ci"X-UserId", _.raw, s => Right(UserId(s)))

    val middleware2 = ContextMiddleware(
        Kleisli((r: Request[IO]) => OptionT.fromOption[IO](r.headers.get[UserId]))
    )
    val cxtRoutes = ContextRoutes.of[UserId,IO]{
        case GET -> Root / "ok" as userId => 
            Ok(s"hello ${userId.raw}")
    }
    val contextService = middleware2(cxtRoutes).orNotFound
    val contextClient = Client.fromHttpApp(contextService)
    val contextRequest = Request[IO](Method.GET, uri"/ok").putHeaders(UserId("Jack"))
    // contextClient.expect[String](contextRequest)

    def run(args: List[String]): IO[ExitCode] = ???