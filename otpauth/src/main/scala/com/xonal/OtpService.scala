package com.xonal

import cats.effect.IO
import com.bastiaanjansen.otp.*
import java.time.Duration
import scala.util.*
import com.xonal.Generator.*

class OtpService(generator: Generator, user: UserDB) {

  if (!user.isSecretUpdated) {
    user.updateSecret(new String(SecretGenerator.generate(512)))
  }

  private val secret: Array[Byte] = user.getSecret.getBytes()

  private val hotpGen =
    new HOTPGenerator.Builder(secret)
      .withPasswordLength(6)
      .withAlgorithm(HMACAlgorithm.SHA256)
      .build()

  private val totpGen =
    new TOTPGenerator.Builder(secret)
      .withHOTPGenerator(builder => {
        builder.withPasswordLength(6)
        builder.withAlgorithm(HMACAlgorithm.SHA256)
      })
      .withPeriod(Duration.ofSeconds(30))
      .build()

  def getToken: IO[String] =
    IO {
      generator match
        case Hotp =>
          hotpGen.generate(user.getCounter)
        case Totp =>
          totpGen.now()
    }

  def verifyCode(code: String): IO[Boolean] =
    IO {
      generator match
        case Hotp =>
          hotpGen
            .verify(code, user.getCounter)
        case Totp =>
          totpGen
            .verify(code)
    }

  def makeBarCodeImg(account: String, issuer: String): IO[Unit] =
    BarCodeService.createQRCode(
      BarCodeService.getGoogleAuthenticatorBarCode(
        new String(secret),
        account,
        issuer,
        generator,
        user.getCounter.toInt - 1
      )
    )
}
