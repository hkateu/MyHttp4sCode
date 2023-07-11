package com.xonal

import cats.effect.* 
import org.http4s.*
import org.http4s.implicits.*
import org.http4s.UriTemplate.*
object UriHandling extends IOApp:
    val uri = uri"http://http4s.org"
    val docs = uri.withPath(path"/docs/0.15")
    val docs2 = uri / "docs" / "0.15"

    val template = UriTemplate(
        authority = Some(Uri.Authority(host = Uri.RegName("http4s.org"))),
        scheme = Some(Uri.Scheme.http),
        path = List(PathElm("docs"), PathElm("0.15"))
    )

    val myUri = template.toUriIfPossible

    //receiving
    
    def run(args: List[String]): IO[ExitCode] = IO(println(uri)).as(ExitCode.Success)