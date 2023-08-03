package com.xonal.config

import com.comcast.ip4s.{Host, Port, host, port}
import io.circe.Decoder
import io.circe.generic.semiauto.*
import ciris.{ConfigDecoder, ConfigValue, file, Effect}
import ciris.circe.circeConfigDecoder
import java.nio.file.Paths

final case class ServerConfig(hostValue: Host, portValue: Port)

object ServerConfig{
  given serverDecoder: Decoder[ServerConfig] = Decoder.instance { h =>
    for
      hostValue <- h.get[String]("host")
      portValue <- h.get[String]("port")
    yield ServerConfig(
      Host.fromString(hostValue).getOrElse(host"0.0.0.0"),
      Port.fromString(portValue).getOrElse(port"5555")
    )
  }

  given serverConfigDecoder: ConfigDecoder[String, ServerConfig] =
    circeConfigDecoder("ServerConfig")
}

val serverConfig: ConfigValue[Effect, ServerConfig] = file(
  Paths.get("oauth/src/main/resources/serverConfig.json")
).as[ServerConfig]