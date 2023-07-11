# Oauth 2.0

## Introduction

Oauth which stands for Open Authorization is an open standard framework that allows the user permit a website or application to interact with another without giving up his or her password.

1. When the user tries to login to an app1, the user is redirected to an authorization server owned by app2.
1. The authorization server provides the user with a prompt, asking the user to grant access to app2 with a list of permissions.
1. Once the prompt is accepted, the user is redirected back to app1 with a single-use authorization code.
1. app1 will send back the authorization code, a client id and a client secret.
1. The authorization server on app2 will respond with a token id and access token
1. app1 can now request the users information from app2's API.

The OAuth standard is defined under [RFC 6749](https://www.ietf.org/rfc/rfc6749.txt), here we'll find an indepth explanation of how the framework works.

## Accessing the github API with OAuth.

In this section we'll connect to Github using OAuth and request for the users information using the Github API.

**Setting Up**

To build this application we will need to add the following to our build.sbt file:

```scala
val scala3Version = "3.2.2"
val Http4sVersion = "0.23.18"
val CirisVersion = "3.2.0"
val CirceVersion = "0.14.1"

val emberServer =     "org.http4s"                %% "http4s-ember-server" % Http4sVersion
val emberClient =     "org.http4s"                %% "http4s-ember-client" % Http4sVersion
val http4sDsl =       "org.http4s"                %% "http4s-dsl"          % Http4sVersion
val ciris =           "is.cir"                    %% "ciris"               % CirisVersion
val cirisCirce =      "is.cir"                    %% "ciris-circe"         % CirisVersion
val circeLibs =  Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % CirceVersion)

  lazy val oauth = project
  .in(file("oauth"))
  .settings(
    name := "oauth",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,
    libraryDependencies ++= Seq(
      emberServer,
      emberClient,
      http4sDsl,
      http4sCirce,
      ciris,
      cirisCirce
    ) ++ circeLibs
  )
```

We'll be using scala3 for this tutorial however one can still use scala2.13 with minimal code changes.

**Registering our OAuth App on Github**
Before we dive into Scala code, we need to register our application on Github.com, Github will provide us with important credentials relevant for our app to function.

Follow the following steps to register our app with Github:

[!step1](pics/step1.png)

1. In the upper right corner of your Github account, click your profile photo, then click settings.

[!step2](pics/step2.png)

1.Click developer settings inthe left sidebar.

[!step3](pics/step3)

1. In the left sidebar, click OAuth apps. If you've never created an app before this button will say, Register a new application.

[!step4](pics/step4)

1. Fill in the necessary fields according to the image above and finally register the application.

[!step5](pics/step5)

1. Copy the Client ID somewhere safe. Also generate a new client secret and save it in the same location.

[!step6](pics/step6)

1. Click the update application button at the bottom of the page.

**Building the Scala application**
In this section we'll build our scala application using Http4s for the routing and serving the application, Circe for json parsing and Ciris for configuration.

**Configuration**
Create a resources folder inside your main folder. Then within it create a `apiConfig.json` file and add the Client Id and Client Secret we saved earlier from Github in the following format.

```json
{
  "key": "0131cd2e9eacae1854c5",
  "secret": "227d0d1f7d11d2bf52c9e9966660ca4a1000718c"
}
```

Also create a serverConfig.json file and save the following contents:

```json
{
  "host": "localhost",
  "port": "8080"
}
```

Here we set host to `localhost` and the port to `8080`.

We will access these values using Ciris in the coming section. At this point our directory structure should look like this:

Let's create an ApiConfig.scala file in the following path, `src/main/scala/com/rockthejvm/config` and add the following information.

```scala
package com.rockthejvm.config
import ciris.Secret

final case class ApiConfig(key: String, secret: Secret[String])
```

The `ApiConfig` final case class will hold the `key` and `secret` values when retrieved from `apiConfig.json`. Ciris provides a `Secret` class which replaces our `secret` value with the first 7 characters of the SHA-1 hash, convinient for passing around sensitive details within our application.

When handling configuration with ciris, values are passed around as `ConfigValue`s. Here's how the official ciris website defines a ConfigValue:

> "`ConfigValue` is the central concept in the library. It represents a single configuration value or a composition of multiple values."

Therefore we'll need to represent our values in this type before we start passing them around the application. Since we saved our values in `.json` files, we will need to decode the json strings, this is where `circe` and `ciris-circe` come into play.

```scala
package com.rockthejvm.config

import java.nio.file.Paths
import ciris.{ConfigDecoder,ConfigValue,file,Effect,Secret}
import ciris.circe.circeConfigDecoder
import io.circe.Decoder
...

object ApiConfig:
    given apiDecoder: Decoder[ApiConfig] = Decoder.instance{h =>
        for
            key <- h.get[String]("key")
            secret <- h.get[String]("secret")
        yield ApiConfig(key,Secret(secret))
        }

    given apiConfigDecoder: ConfigDecoder[String, ApiConfig] =
        circeConfigDecoder("ApiConfig")

val apiConfig: ConfigValue[Effect, ApiConfig] = file(Paths.get("oauth/src/main/resources/apiConfig.json")).as[ApiConfig]
```

This section is abit confusing so pay attention, we will start from the bottom up.

apiConfig is of type `ConfigValue[Effect, ApiConfig]` which is the format we will use passing config values. `Ciris` provides a `file()` function that takes the path to our configuration file. This function returns a type `ConfigValue[Effect, String]`, we use the `as()` function to convert our value to a `ConfigValue[Effect, ApiConfig]` type however it requires an implicit `ConfigDecoder` value, here's the function signature:

```scala
final def as[B](implicit decoder: ConfigDecoder[String, B]): ConfigValue[Effect, B]
```

We defined an implicit `ConfigDecoder[String, ApiConfig]` within the `ApiConfig` companion object, in the previous line, the `apiConfigDecoder` `given` is created using `ciris-circe`'s `circeConfigDecoder()` function however it also requires an implicit `Decoder` that we also defined in the previous lines, here's the function signature.

```scala
final def circeConfigDecoder[A](typeName: String)(implicit decoder: Decoder[A]): ConfigDecoder[String, A]
```

`circeConfigDecoder` also takes the type name as an argument, which we provide as the "ApiConfig" string.

The `apiDecoder` `given` provides the ability to parse our json string to an `ApiConfig` case class using `circe`'s `Decoder.instance` function.

Let's create `ServerConfig.scala` file and add the following content:

```scala
package com.xonal.config

import com.comcast.ip4s.{Host,Port, host, port}
import io.circe.Decoder
import io.circe.generic.semiauto.*
import ciris.{ConfigDecoder,ConfigValue,file,Effect}
import ciris.circe.circeConfigDecoder
import java.nio.file.Paths

final case class ServerConfig(hostValue: Host, portValue: Port)

object ServerConfig{
    given serverDecoder: Decoder[ServerConfig] = Decoder.instance{h =>
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

val serverConfig: ConfigValue[Effect, ServerConfig] = file(Paths.get("oauth/src/main/resources/serverConfig.json")).as[ServerConfig]
```

The logic for `ServerConfig.scala` is similar to `ApiConfig.scala`, we create a `ServerConfig` case class to hold the configuration data and then a companion object to handle json parsing with ciris. For this section, we use the `fromString()` method available in `Host` and `Port` for our serverDecoder then chain the `getOrElse()` method to set default values for our configuration.

The last file in our config folder is Config.scala.

```scala
package com.xonal.config

import ciris.{ConfigValue,Effect}
import cats.syntax.all.*

final case class Config(api: ApiConfig, server: ServerConfig)

val configuration: ConfigValue[Effect, Config] =
    (
        apiConfig,
        serverConfig
    ).parMapN(Config.apply)
```

Here the Config case class acts as a master configuration class through which we access all the other configurations. In case more configurations are needed, they are eventually added to `Config`.
We use the parMapN method from cats syntax to add the `serverConfig` and `apiConfig` configurations to the `Config` apply method.
