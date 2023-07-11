package com.xonal

import cats.effect.*
// import java.io.FileInputStream
// import java.security.KeyStore
// import com.onelogin.saml2.Auth
// import com.onelogin.saml2.model.KeyStoreSettings
// import org.http4s.*
// import org.http4s.dsl.io.*
// import io.circe.generic.auto.* 
// import io.circe.syntax.* 
// import org.http4s.circe.* 
// import org.http4s.headers.Cookie
// import io.circe.* 
// import io.circe.parser.*
// import org.http4s.ember.server.*
// import com.comcast.ip4s.*
// import com.onelogin.saml2.factory.SamlMessageFactory
// import com.onelogin.saml2.authn.*
// import org.http4s.server.middleware.*
// import javax.net.ssl.KeyManagerFactory
// import javax.net.ssl.TrustManagerFactory
// import javax.net.ssl.SSLContext
// import java.security.SecureRandom
// import fs2.io.net.tls.TLSContext
// import java.nio.file.Paths
// import org.http4s.server.Server

object SamlAuth extends IOApp:
    // val keyStoreFile = "authentication/src/main/resources/senderKeystore.jks"
    // val alias = "https://xonal.live"
    // val storePass = "Herbert0009!"
    // val keyPassword = "Herbert0009!"

    // val ks = KeyStore.getInstance("JKS")
    // ks.load(new FileInputStream(keyStoreFile),storePass.toCharArray())

    // //Certificate
    // val keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
    // keyManagerFactory.init(ks,storePass.toCharArray())

    // val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
    // trustManagerFactory.init(ks)

    // val sslContext = SSLContext.getInstance("TLS")
    // sslContext.init(keyManagerFactory.getKeyManagers, trustManagerFactory.getTrustManagers, new SecureRandom())

    // val myTlsContext = TLSContext.Builder.forAsync[IO].fromKeyStore(ks, keyPassword.toCharArray())

    // val keyStoreSettings = new KeyStoreSettings(ks, alias, keyPassword)
    // val auth = new Auth(keyStoreSettings)

    // val settings = auth.getSettings();
    // settings.setSPValidationOnly(true);
    // val errors = settings.checkSettings();

    // case class Session(nameId: String, nameIdFormat: String, sessionIndex: String, nameidNameQualifier: String, nameidSPNameQualifier: String)

    // object Session:
    //     given decoder: Decoder[Session] = Decoder.instance{h =>
    //     for 
    //         nameId <- h.get[String]("nameId")
    //         nameIdFormat <- h.get[String]("nameIdFormat")
    //         sessionIndex <- h.get[String]("sessionIndex")
    //         nameidNameQualifier <- h.get[String]("nameidNameQualifier")
    //         nameidSPNameQualifier <- h.get[String]("nameidSPNameQualifier")
    //     yield Session(nameId, nameIdFormat, sessionIndex, nameidNameQualifier, nameidSPNameQualifier)  
    // }

    // given decoder: EntityDecoder[IO, Session] = jsonOf[IO,Session]

    // def checkSessionCookie(cookie: Cookie):Option[RequestCookie] = 
    //     cookie.values.toList.find(_.name == "samlValues")

    // object SamlQueryParamMatcher extends QueryParamDecoderMatcher[String]("SAMLRequest")

    // val samlRoutes: HttpRoutes[IO] = 
    // HttpRoutes.of {
    //     case GET -> Root / "metadata" =>
    //         if (errors.isEmpty()) {
    //             val metadata = settings.getSPMetadata()
    //             Ok(metadata)
    //         } else{
    //             Ok(errors.toString)
    //         }
    //     case GET -> Root / "login" => 
    //         auth.login("http://localhost:8080/callback", new AuthnRequestParams(false, false, true))
    //         Ok("log In Attempt")
    //     case GET -> Root / "callback" :? SamlQueryParamMatcher(samlrequest) => 
    //         Ok(s"saml results: ${SamlResponse(settings,"https://localhost:8080/callback", samlrequest).getNameId()}")
	// 		// val nameidFormat = samlResponse.getNameIdFormat()
	// 		// val nameidNameQualifier = samlResponse.getNameIdNameQualifier()
	// 		// val nameidSPNameQualifier = samlResponse.getNameIdSPNameQualifier()
	// 		// val authenticated = true
	// 		// val attributes = samlResponse.getAttributes()
	// 		// val sessionIndex = samlResponse.getSessionIndex()
	// 		// val sessionExpiration = samlResponse.getSessionNotOnOrAfter()
	// 		// val lastMessageId = samlResponse.getId()
	// 		// val lastMessageIssueInstant = samlResponse.getResponseIssueInstant()
	// 		// val lastAssertionId = samlResponse.getAssertionId()
	// 		// val lastAssertionNotOnOrAfter = samlResponse.getAssertionNotOnOrAfter()
            
            
    //     case GET -> Root / "acs" => 
    //         auth.processResponse()
    //         if(!auth.isAuthenticated()){
    //             Ok("Not Authenticated")
    //         }

    //         val errors = auth.getErrors()

    //         if (!errors.isEmpty()) {
    //             if(auth.isDebugActive()){
    //                 val errorReason = auth.getLastErrorReason()
    //                 if(errorReason != null && !errorReason.isEmpty()){
    //                     Ok(s"Error: ${auth.getLastErrorReason()}")
    //                 }
    //             }

    //             Ok(s"${errors.toString}, Please log in")
    //         } else{
    //             val authSession = Session(
    //                 auth.getNameId(), 
    //                 auth.getNameIdFormat(), 
    //                 auth.getSessionIndex(), 
    //                 auth.getNameIdNameQualifier(), 
    //                 auth.getNameIdSPNameQualifier()
    //             )

    //             Ok("Session data stored").map(_.addCookie(ResponseCookie("samlValues", authSession.asJson.spaces2)))
    //         }
            
    //     case req @ GET -> Root / "logout" =>
    //         val authHeader: Option[Cookie] = req.headers.get[Cookie]
    //         authHeader.fold(Ok("No cookies")){cookie =>
    //             checkSessionCookie(cookie).fold(Ok("No saml values ")){saml =>
    //                 val sValues = decode[Session](saml.content)
    //                 // val auth = new Auth(keyStoreSettings)
    //                 sValues match
    //                     case Right(sValues) =>
    //                         auth.logout(null, sValues.nameId, sValues.sessionIndex, sValues.nameIdFormat, sValues.nameidNameQualifier, sValues.nameidSPNameQualifier)
    //                         Ok("logged Out")
    //                     case Left(value) => 
    //                         Ok("Log out failed")
    //             }
    //         } 
    // }

    // def server(tlsContext: IO[TLSContext[IO]]): IO[Resource[IO, Server]] =
    //     tlsContext.map{cxt =>
    //         EmberServerBuilder
    //             .default[IO]
    //             .withHost(ipv4"0.0.0.0")
    //             .withPort(port"8080")
    //             .withTLS(cxt)
    //             .withHttpApp(samlRoutes.orNotFound)
    //             .build
    //     }
    // //openssl pkcs12 -info -in senderKeystore.p12 --nodes
    // //to get the private key

    def run(args: List[String]): IO[ExitCode] = ???
        // server(myTlsContext).flatMap(s => s.use(_ => IO.never)).as(ExitCode.Success)