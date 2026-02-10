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
package com.snowplowanalytics.refererparser

import cats.Eval
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.specs2.Specification

class MediaReferersTest extends Specification {
  def is = s2"""
  Media referers loaded from configuration should
    parse video medium from YouTube $e1
    parse video medium from YouTube short URL $e2
    extract video ID from query parameters $e3
    parse podcast medium from Spotify $e4
    extract episode from query parameters $e5
  """

  val resource   = getClass.getResource("/referers-media.json").getPath
  val ioParser   = CreateParser[IO].create(resource).unsafeRunSync().fold(throw _, identity)
  val evalParser = CreateParser[Eval].create(resource).value.fold(throw _, identity)

  def e1 = {
    val refererUri = "https://youtube.com/watch"
    val expected   = Some(ExternalReferer("video", "YouTube", None))
    (expected shouldEqual ioParser.parse(refererUri)) and
      (expected shouldEqual evalParser.parse(refererUri))
  }

  def e2 = {
    val refererUri = "https://youtu.be/dQw4w9WgXcQ"
    val expected   = Some(ExternalReferer("video", "YouTube", None))
    (expected shouldEqual ioParser.parse(refererUri)) and
      (expected shouldEqual evalParser.parse(refererUri))
  }

  def e3 = {
    val refererUri = "https://youtube.com/watch?v=abc123xyz"
    val expected   = Some(ExternalReferer("video", "YouTube", Some("abc123xyz")))
    (expected shouldEqual ioParser.parse(refererUri)) and
      (expected shouldEqual evalParser.parse(refererUri))
  }

  def e4 = {
    val refererUri = "https://spotify.com/shows/12345"
    val expected   = Some(ExternalReferer("podcast", "Spotify", None))
    (expected shouldEqual ioParser.parse(refererUri)) and
      (expected shouldEqual evalParser.parse(refererUri))
  }

  def e5 = {
    val refererUri = "https://spotify.com/shows/12345?episode=ep789"
    val expected   = Some(ExternalReferer("podcast", "Spotify", Some("ep789")))
    (expected shouldEqual ioParser.parse(refererUri)) and
      (expected shouldEqual evalParser.parse(refererUri))
  }
}
