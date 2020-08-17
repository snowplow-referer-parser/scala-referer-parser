/**
  * Copyright 2012-2020 Snowplow Analytics Ltd
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
val root = (project in file("."))
  .enablePlugins(ScalaUnidocPlugin, GhpagesPlugin)
  .settings(
    name := "scala-referer-parser",
    organization := "com.snowplowanalytics",
    version := "1.0.0",
    description := "Library for extracting marketing attribution data from referer URLs",
    scalaVersion := "2.13.3",
    crossScalaVersions := Seq("2.11.12", "2.12.8", "2.13.3"),
    javacOptions := BuildSettings.javaCompilerOptions,
    libraryDependencies ++= Seq(
      Dependencies.Libraries.catsCore,
      Dependencies.Libraries.catsEffect,
      Dependencies.Libraries.collectionCompat,
      Dependencies.Libraries.specs2Core,
      Dependencies.Libraries.specs2Scalacheck
    ),
    libraryDependencies ++= {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, scalaMajor)) if scalaMajor == 11 =>
          Seq(
            Scala211Dependencies.Libraries.circeCore,
            Scala211Dependencies.Libraries.circeGeneric,
            Scala211Dependencies.Libraries.circeParser
          )
        case _ =>
          Seq(
            Dependencies.Libraries.circeCore,
            Dependencies.Libraries.circeGeneric,
            Dependencies.Libraries.circeParser
          )
      }
    }
  )
  .settings(BuildSettings.publishSettings)
  .settings(BuildSettings.docSettings)
