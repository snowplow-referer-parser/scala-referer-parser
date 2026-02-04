/**
 * Copyright 2012-present Snowplow Analytics Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
// Sbt
import sbt.Keys._
import sbt._

// scalafmt
import org.scalafmt.sbt.ScalafmtPlugin.autoImport._

// dynver plugin
import sbtdynver.DynVerPlugin.autoImport._

//Scaladocs
import com.typesafe.sbt.site.SitePlugin.autoImport._
import com.typesafe.sbt.site.SiteScaladocPlugin.autoImport.SiteScaladoc

object BuildSettings {

  lazy val scala212 = "2.12.20"
  lazy val scala213 = "2.13.16"

  lazy val buildSettings = Seq(
    organization := "com.snowplowanalytics",
    name := "scala-referer-parser",
    description := "Library for extracting marketing attribution data from referer URLs",
    scalaVersion := scala213,
    crossScalaVersions := List(scala212, scala213),
    scalafmtConfig := file(".scalafmt.conf"),
    scalafmtOnCompile := false,
    scalacOptions ++= scalacOptionsVersion(scalaVersion.value),
    resolvers ++= Seq(
      "Snowplow Analytics Maven repo".at("http://maven.snplow.com/releases/").withAllowInsecureProtocol(true)
    )
  )

  def scalacOptionsVersion(scalaVersion: String): Seq[String] = {
    // Scala 2.12 needs -Ywarn-macros:after to match the default compiler behaviour of scala 2.13.
    val versionSpecificOptions = CrossVersion.partialVersion(scalaVersion) match {
      case Some((2, scalaMajor)) if scalaMajor == 12 => Seq("-Ywarn-macros:after")
      case _                                         => Nil
    }
    Seq(
      "-Wconf:origin=scala.collection.compat.*:s"
    ) ++ versionSpecificOptions
  }

  lazy val publishSettings = Seq[Setting[_]](
    publishArtifact := true,
    Test / publishArtifact := false,
    pomIncludeRepository := { _ => false },
    homepage := Some(url("https://snowplow.io")),
    licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0.html")),
    ThisBuild / dynverVTagPrefix := false, // Otherwise git tags required to have v-prefix
    developers := List(
      Developer(
        "Snowplow Analytics Ltd",
        "Snowplow Analytics Ltd",
        "support@snowplowanalytics.com",
        url("https://snowplow.io")
      )
    )
  )

  lazy val docSettings = Seq(
    SiteScaladoc / siteSubdirName := s"${version.value}"
  )
}
