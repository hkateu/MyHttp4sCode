package com.xonal
import cats.effect.{IO, IOApp, ExitCode}
import org.http4s.* 
import org.http4s.dsl.io.*
import org.http4s.implicits.*
import org.http4s.server.middleware.*
import org.http4s.headers.*
import scala.concurrent.duration.*

object Hsts extends IOApp:
    val service = HttpRoutes.of[IO]{
        case _ => Ok("ok") 
    }

    val request = Request[IO](Method.GET, uri"/")

    val responseOk = service.orNotFound(request)

    val hstsService = HSTS(service)

    val responseHSTS = hstsService.orNotFound(request)

    val hstsHeader = `Strict-Transport-Security`.unsafeFromDuration(30.days,includeSubDomains = true,preload = true)

    val hstsServiceCustom = HSTS(service, hstsHeader)

    val responseCustom = hstsServiceCustom.orNotFound(request)

    def run(args: List[String]): IO[ExitCode] = responseCustom.map(res => println(res.headers)).as(ExitCode.Success)