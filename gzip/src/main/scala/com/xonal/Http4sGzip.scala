package xonal

import cats.effect.{IOApp, IO, ExitCode}
import org.http4s.*
import org.http4s.dsl.io.* 
import org.http4s.implicits.*
import org.http4s.server.middleware.*

object Http4sGzip extends IOApp:

    val service = HttpRoutes.of[IO]{
        case _ => Ok("I repeate myself when I'm under stress." * 3)
    }

    val request = Request[IO](Method.GET, uri"/")
    val response = service.orNotFound(request)
    val body = response.as[String]

    val serviceZip = GZip(service)
    val respNormal = serviceZip.orNotFound(request)
    val bodyNormal = respNormal.as[String]

    val requestZip = request.putHeaders("Accept-Encoding" -> "gzip")
    override def run(args: List[String]): IO[ExitCode] = bodyNormal("Hello").map(println).as(ExitCode.Success)
