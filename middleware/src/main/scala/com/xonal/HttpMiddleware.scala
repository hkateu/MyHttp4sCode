package com.xonal

import cats.data.Kleisli
import cats.effect.*
import cats.syntax.all.* 
import org.http4s.* 
import org.http4s.dsl.io.* 
import org.http4s.implicits.* 

object HttpMiddleware extends IOApp:
    def myMiddle(service: HttpRoutes[IO], header: Header.ToRaw): HttpRoutes[IO] = 
        Kleisli{(req: Request[IO]) => 
            service(req).map{
                case Status.Successful(resp) =>
                    resp.putHeaders(header)
                case resp => resp
            }
        }

    val service = HttpRoutes.of[IO]{
        case GET -> Root / "bad" => 
            BadRequest()
        case _ => Ok()
    }    

    val goodRequest = Request[IO](Method.GET, uri"/")
    val badRequest = Request[IO](Method.GET, uri"/bad")

    service.orNotFound(goodRequest)
    service.orNotFound(badRequest)

    val modifiedService = myMiddle(service, "SomeKey" -> "SomeValue")
    modifiedService.orNotFound(goodRequest)
    modifiedService.orNotFound(badRequest)

    //incase middle ware will be used in multiple places, its better to implement it as an object
    object MyMiddle:
        def addHeader(resp: Response[IO], header: Header.ToRaw) = 
            resp match
                case Status.Successful(resp) =>
                    resp.putHeaders(header)
                case resp => resp

        def apply(service: HttpRoutes[IO], header: Header.ToRaw) = 
            service.map(addHeader(_,header)) 
    end MyMiddle

    val newService = MyMiddle(service, "SomeKey" -> "SomeValue") 

    //composing services with middleware
    val apiService = HttpRoutes.of[IO]{
        case GET -> Root / "api" => 
            Ok()
    }          

    val anotherService = HttpRoutes.of[IO]{
        case GET -> Root / "another" => 
            Ok()
    }        

    val aggregateService = apiService <+> MyMiddle(service <+> anotherService, "SomeKey" -> "SomeValue")

    val apiRequest = Request[IO](Method.GET, uri"/api")
    aggregateService.orNotFound(apiRequest) //will not have headers added because it doesnt run through the MyMiddle middleware
    

    override def run(arg: List[String]): IO[ExitCode] = IO(println("Middleware say hi")).as(ExitCode.Success)