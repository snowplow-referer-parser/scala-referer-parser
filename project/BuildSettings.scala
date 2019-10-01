/**
 * Copyright 2012-2019 Snowplow Analytics Ltd
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
import Keys._

import bintray.BintrayPlugin._
import bintray.BintrayKeys._

object BuildSettings {

  // Basic settings for our app
  lazy val basicSettings = Seq[Setting[_]](
    organization  := "com.snowplowanalytics",
    version       := "0.3.1",
    description   := "Library for extracting marketing attribution data from referer URLs",
    scalaVersion  := "2.11.1",
    crossScalaVersions := Seq("2.10.4", "2.11.1"),
    scalacOptions := Seq("-deprecation", "-encoding", "utf8"),
    resolvers     ++= Dependencies.resolutionRepos
  )

  lazy val publishSettings = bintraySettings ++ Seq(
    publishMavenStyle := true,
	publishArtifact := true,
	publishArtifact in Test := false,
	licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0.html")),
	bintrayOrganization := Some("snowplow"),
	bintrayRepository := "snowplow-maven",
	pomIncludeRepository := { _ => false },
	homepage := Some(url("http://snowplowanalytics.com")),
	scmInfo := Some(ScmInfo(url("https://github.com/snowplow-referer-parser/scala-referer-parser"),
      "scm:git@github.com:snowplow-referer-parser/scala-referer-parser.git")),
	pomExtra := (
      <developers>
        <developer>
          <name>Snowplow Analytics Ltd</name>
            <email>support@snowplowanalytics.com</email>
            <organization>Snowplow Analytics Ltd</organization>
            <organizationUrl>http://snowplowanalytics.com</organizationUrl>
        </developer>
      </developers>)
  )

  lazy val buildSettings = basicSettings ++ publishSettings
}
