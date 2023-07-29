package com.xonal.config

import ciris.{ConfigValue, Effect}
import cats.syntax.all.*
import cats.effect.IO

final case class Config(api: ApiConfig, server: ServerConfig)

val configuration: ConfigValue[Effect, Config] =
  (
    apiConfig,
    serverConfig
  ).parMapN(Config.apply)
