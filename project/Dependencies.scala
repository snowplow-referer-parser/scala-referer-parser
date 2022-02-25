/**
  * Copyright 2012-2022 Snowplow Analytics Ltd
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *      http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
import sbt._

object Dependencies {
  object V {
    val catsCore         = "2.7.0"
    val catsEffect       = "3.3.5"
    val circe            = "0.14.1"
    val collectionCompat = "2.1.6"
    val specs2           = "4.13.2"
  }

  object Libraries {
    val catsCore         = "org.typelevel"          %% "cats-core"               % V.catsCore
    val catsEffect       = "org.typelevel"          %% "cats-effect"             % V.catsEffect
    val circeCore        = "io.circe"               %% "circe-core"              % V.circe
    val circeGeneric     = "io.circe"               %% "circe-generic"           % V.circe
    val circeParser      = "io.circe"               %% "circe-parser"            % V.circe
    val collectionCompat = "org.scala-lang.modules" %% "scala-collection-compat" % V.collectionCompat
    val specs2Core       = "org.specs2"             %% "specs2-core"             % V.specs2 % Test
    val specs2Scalacheck = "org.specs2"             %% "specs2-scalacheck"       % V.specs2 % Test
  }
}
