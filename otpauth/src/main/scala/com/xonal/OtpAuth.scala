import cats.effect.*
import cats.implicits.*
import org.http4s.*
import org.http4s.dsl.io.*
import com.comcast.ip4s.*
import org.http4s.ember.server.EmberServerBuilder
import com.xonal.OtpInteractiveService
import com.xonal.UserDB
import com.xonal.Generator.*
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

object OtpAuth extends IOApp {
  val user = UserDB("herbert", "hkateu@gmail.com")
  val tokenService = new OtpInteractiveService(Hotp, user)

  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  val routes: HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case GET -> Root / "login" =>
        // create carcodeimage
        tokenService.otpService
          .makeBarCodeImg("hkateu@gmail.com", "herbert")
          .attempt
          .flatMap {
            // successful barcode image creation
            case Right(_) =>
              // Log token
              tokenService.otpService.getToken
                .flatMap { token =>
                  logger.info(s"Token value: $token")
                }
                .handleErrorWith { ex =>
                  logger.error(s"GetToken Error: ${ex.getMessage}")
                } *>
                // send email
                tokenService.send2FA(user).flatMap {
                  // successful email
                  case Right(res) =>
                    logger.info(s"Response Body: ${res.getBody()}") *>
                      logger
                        .info(
                          s"Response Status Code: ${res.getStatusCode()}"
                        ) *>
                      logger.info(s"Response Headers: ${res.getHeaders()}") *>
                      Ok(
                        "We sent you an email, please follow the steps to complete the signin process."
                      )
                  // failed email
                  case Left(ex) =>
                    logger.error(s"Response Error: $ex") *>
                      Ok("Oops, something went wrong, please try again later.")
                }
            // failed bar code image creation
            case Left(ex) =>
              logger.error(s"QR Code Error: ${ex.getMessage}") *>
                Ok("Error occurred generating QR Code...")
          }
      case GET -> Root / "code" / value =>
        // verify code
        tokenService.otpService
          .verifyCode(value)
          .flatMap { result =>
            if (result) then
              IO(user.incrementCounter) *>
              IO(user.isSecretUpdated = false) *>
              Ok(s"Code verification passed")
            else Ok(s"Code verification failed")

          }
          .handleErrorWith { ex =>
            logger.error(s"Verification Error: ${ex.getMessage}") *>
              Ok("An error occured during verification")
          }
    }

  val server =
    EmberServerBuilder
      .default[IO]
      .withHost(ipv4"0.0.0.0")
      .withPort(port"8080")
      .withHttpApp(routes.orNotFound)
      .build

  override def run(args: List[String]): IO[ExitCode] =
    server.use(_ => IO.never).as(ExitCode.Success)
}
