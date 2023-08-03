package com.xonal.config

import java.nio.file.Paths
import ciris.{ConfigDecoder, ConfigValue, file, Effect, Secret}
import ciris.circe.circeConfigDecoder
import io.circe.Decoder

final case class ApiConfig(key: String, secret: Secret[String])

object ApiConfig{
  given apiDecoder: Decoder[ApiConfig] = Decoder.instance { h =>
    for
      key <- h.get[String]("key")
      secret <- h.get[String]("secret")
    yield ApiConfig(key, Secret(secret))
  }

  given apiConfigDecoder: ConfigDecoder[String, ApiConfig] =
    circeConfigDecoder("ApiConfig")
}

val apiConfig: ConfigValue[Effect, ApiConfig] = file(
  Paths.get("oauth/src/main/resources/apiConfig.json")
).as[ApiConfig]