package com.xonal

import com.sendgrid.{SendGrid, Method, Request, Response}
import com.sendgrid.helpers.mail.objects.{Email, Content, Attachments}
import com.sendgrid.helpers.mail.Mail
import java.nio.file.{Paths, Files}
import java.util.Base64
import scala.util.*
import cats.effect.IO

class OtpInteractiveService(generator: Generator, user: UserDB) {
  def otpService = new OtpService(generator, user)

  def send2FA(sendto: UserDB): IO[Either[String, Response]] = {
    val from = new Email("hkateu@gmail.com")
    val to = new Email(sendto.email)

    val subject = "OtpAuth Test Application"
    val message = s"""
            |<H3>Website Login</H3>
            |<p>Please complete the login process using google authenticator</p>
            |<p>Scan the QR Barcode attached and send us the token number.</p>
            """.stripMargin

    val file = Paths.get("barCode.png")
    val content = new Content("text/html", message)

    val mail = new Mail(from, subject, to, content)
    val attachments = new Attachments()
    attachments.setFilename(file.getFileName().toString())
    attachments.setType("application/png")
    attachments.setDisposition("attachment")
    val attachmentContentBytes = Files.readAllBytes(file)
    val attachmentContent =
      Base64.getEncoder().encodeToString(attachmentContentBytes)
    attachments.setContent(attachmentContent)
    mail.addAttachments(attachments)

    val sg = new SendGrid(Properties.envOrElse("SENDGRID_API_KEY", "undefined"))
    val request = new Request()
    val response: IO[Either[String, Response]] =
      IO {
        Try {
          request.setMethod(Method.POST)
          request.setEndpoint("mail/send")
          request.setBody(mail.build())
          val res = sg.api(request)
          res
        }.toEither.left.map(_.getMessage)
      }

    response
  }
}
