package com.xonal

import cats.effect.*
import org.http4s.*
import org.http4s.headers.`Content-Type`
import org.http4s.dsl.io.*
import cats.implicits.*
import cats.data.*

object EntityHandling extends IOApp:
    sealed trait Resp
    case class Audio(body: String) extends Resp
    case class Video(body: String) extends Resp

    val response = Ok("").map(_.withContentType(`Content-Type`(MediaType.audio.ogg)))
    val audioDec = EntityDecoder.decodeBy(MediaType.audio.ogg) {(m: Media[IO]) => 
        EitherT{
            m.as[String].map(s => Audio(s).asRight[DecodeFailure])
        }    
    }

    val videoDec = EntityDecoder.decodeBy(MediaType.video.ogg){(m: Media[IO]) => 
        EitherT{
            m.as[String].map(s => Video(s).asRight[DecodeFailure])
        }    
    }

    given bothDec: EntityDecoder[IO,Resp] = audioDec.widen[Resp].orElse(videoDec.widen[Resp])



    def run(args: List[String]): IO[ExitCode] = response.flatMap(_.as[Resp]).as(ExitCode.Success)