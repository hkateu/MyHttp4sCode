import cats.effect.* 
import org.http4s.*
import org.http4s.dsl.io.* 
import org.http4s.server.*  
import org.http4s.ember.server.*
import com.comcast.ip4s.*
import org.http4s.server.middleware.authentication.DigestAuth 
import org.http4s.server.middleware.authentication.DigestAuth.Md5HashedAuthStore

object DigestExample extends IOApp:
    case class User(id: Long, name: String)

    val ha1: IO[String] = Md5HashedAuthStore.precomputeHash[IO]("username","http://localhost:8080/welcome","password")
    val funcPass: String => IO[Option[(User, String)]] = (usr_name: String) => 
        usr_name match
            case "username" => ha1.flatMap(hash => IO(Some(User(1,"username"), hash))) 
            case _ => IO(None)

    
    val middleware: IO[AuthMiddleware[IO, User]] = DigestAuth.applyF[IO,User]("http://localhost:8080/welcome", Md5HashedAuthStore(funcPass))

    val authedRoutes: AuthedRoutes[User,IO] = 
        AuthedRoutes.of{
            case GET -> Root / "welcome" as user =>
                Ok(s"Welcome, ${user.name}")
        }

    val digestService: IO[HttpRoutes[IO]] = 
        middleware.map(wrapper => wrapper(authedRoutes))

    def server(service: IO[HttpRoutes[IO]]): IO[Resource[IO, Server]] =
        service.map{svc =>
            EmberServerBuilder
                .default[IO]
                .withHost(ipv4"0.0.0.0")
                .withPort(port"8080")
                .withHttpApp(svc.orNotFound)
                .build
        }

    override def run(args: List[String]): IO[ExitCode] = server(digestService).flatMap(s => s.use(_ => IO.never)).as(ExitCode.Success)
        
        // server.use(_ => IO.never).as(ExitCode.Success)