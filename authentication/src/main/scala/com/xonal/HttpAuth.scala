package com.xonal
import cats.*
import cats.effect.*
import cats.implicits.* 
import cats.data.* 
import org.http4s.* 
import org.http4s.dsl.io.* 
import org.http4s.server.*  
import org.http4s.implicits.* 
import org.http4s.ember.server.*
import com.comcast.ip4s.*
import org.typelevel.ci.CIString
import org.http4s.Credentials
import org.http4s.headers.Authorization
import scala.io.Codec
import scala.util.Random
import org.http4s.headers.Cookie
import org.http4s.server.middleware.authentication.{DigestAuth}

object HttpAuth extends IOApp:
    case class User(id: Long, name: String)

    // val authUser: Kleisli[[User] =>> OptionT[IO,User], Request[IO], User] = 
    //     Kleisli(_ => OptionT.liftF(IO(User(1,"Herbert"))))

    // [User] =>> OptionT[IO,User],
    val authUser: Kleisli[[User] =>> OptionT[IO,User], Request[IO], User] = Kleisli{req => 
        // val authHeader = req.headers.get(Credentials.Token(CIString("Authorization")))
        val authHeader: Option[Authorization] = req.headers.get[Authorization]
        OptionT.liftF(IO{
            authHeader.flatMap{
                case Authorization(BasicCredentials(value)) =>  Some(User(1,value._2))
                case _ => Some(User(0,"ErrorName"))
        }.get})
    }

    val funcPass: String => IO[Option[(User, String)]] = (hashedVal: String) => 
        hashedVal match
            case "Herbert" => IO(Some(User(1,"Herbert"),"password"))
            case _ => IO(None)

    val dig:AuthMiddleware[IO, User] = DigestAuth[IO,User]("localhost:8080/welcome", funcPass)
    
    val middleware: AuthMiddleware[IO,User] = 
        AuthMiddleware(authUser)

    val authedRoutes: AuthedRoutes[User,IO] = 
        AuthedRoutes.of{
            case GET -> Root / "welcome" as user =>
                Ok(s"Welcome, ${user.name}")
        }
    
    val service: HttpRoutes[IO] = 
        middleware(authedRoutes)

    val digestService: HttpRoutes[IO] = 
        dig(authedRoutes)

    //composing authenticated routes
    val spanishRoutes: AuthedRoutes[User, IO] = 
        AuthedRoutes.of{
            case GET -> Root / "hola" as user => 
                Ok(s"Hola, ${user.name}")
        }

    val frenchRoutes: HttpRoutes[IO] = 
        HttpRoutes.of {
            case GET -> Root / "bonjour" =>
                Ok(s"bonjour")
        }

    // val serviceSpanish: HttpRoutes[IO] = 
    //     middleware(spanishRoutes) <+> frenchRoutes

    val serviceRouter = 
        Router(
            "/spanish" -> middleware(spanishRoutes),
            "/french" -> frenchRoutes
        )

    //using fallthrough
    val middlewareWithFallThrough: AuthMiddleware[IO,User] = 
        AuthMiddleware.withFallThrough(authUser)

    val middlewareWithFailure: AuthMiddleware[IO,User] =
        AuthMiddleware.noSpider(authUser, (req: Request[IO]) => IO(Response[IO](Status.Locked))) 

    val serviceSF: HttpRoutes[IO] = 
        frenchRoutes <+> middlewareWithFallThrough(spanishRoutes)

    val serviceRouter2 = 
        Router(
            "/spanish" -> middlewareWithFailure(spanishRoutes),
            "/french" -> frenchRoutes
        )

    //Returning an Error
    val authUserEither: Kleisli[IO, Request[IO], Either[String, User]] = Kleisli{req =>
        val authHeader = req.headers.get[Authorization]
        authHeader match
            case Some(value) => value match
                case Authorization(BasicCredentials(value)) =>  IO(Right(User(1,value._1)))
                case _ => IO(Left("ErrorName"))
            case None => IO(Left("Unauthorized"))
    }

    val onFailure: AuthedRoutes[String, IO] = Kleisli{(req: AuthedRequest[IO,String]) => 
        req.req match
            case _ => OptionT.pure[IO](Response[IO](status = Status.Unauthorized))    
    }
        
    val authMiddleware: AuthMiddleware[IO,User] = AuthMiddleware(authUserEither, onFailure)

    val serviceKleisli: HttpRoutes[IO] = authMiddleware(authedRoutes)


    //Implementing authUser
    //cookies
    val sessionKey = Codec.toUTF8(Random.alphanumeric.take(20).mkString(""))
   
    val clock = java.time.Clock.systemUTC  

    //figure out how to do a request form
    def verifyLogin(request: Request[IO]): IO[Either[String,User]] = ???

    // val logIn: Kleisli[IO, Request[IO], Response[IO]] = Kleisli({request =>
    //     verifyLogin(request: Request[IO]).flatMap(_ match
    //         case Left(error) => 
    //             Forbidden(error)
    //         case Right(user) => 
    //             val message = "signedToken"
    //             Ok("Logged in!").map(_.addCookie(ResponseCookie("authcookie", message)))
    //     )    
    // })

    //retrieve value in authUser
    //  def retrieveUser: Kleisli[IO, Long, User] = Kleisli(id => IO(???))
    // val authUserCookie: Kleisli[IO, Request[IO], Either[String,User]] = Kleisli({request =>
    //     val message = 
    //         for 
    //             header <- request.headers.get[Cookie].toRight("Cookie parsing error")
    //             cookie <- header.values.toList.find(_.name == "authcookie")
    //             token <- Right("validatedSignedToken")
    //             message <- Either.catchOnly[NumberFormatException](token).leftMap(_.toString)
    //         yield message
    //     message.traverse(retrieveUser.run)
    // })

    val server = EmberServerBuilder
        .default[IO]
        .withHost(ipv4"0.0.0.0")
        .withPort(port"8080")
        .withHttpApp(serviceKleisli.orNotFound)
        .build

    override def run(args: List[String]): IO[ExitCode] = server.use(_ => IO.never).as(ExitCode.Success)