/**
  * Copyright 2018-2022 Snowplow Analytics Ltd
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
package com.snowplowanalytics.refererparser

import cats.Eval
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import io.circe._
import io.circe.generic.semiauto._
import io.circe.parser._
import org.specs2.mutable.Specification

import java.net.URI
import scala.io.Source._

case class TestCase(
  spec: String,
  uri: String,
  medium: String,
  source: Option[String],
  term: Option[String],
  known: Boolean
)

class JsonParseTest extends Specification {
  implicit val testCaseDecoder: Decoder[TestCase] = deriveDecoder[TestCase]

  val testString = fromFile("src/test/resources/referer-tests.json").getLines().mkString

  // Convert the JSON to a List of TestCase
  val eitherTests = for {
    doc <- parse(testString)
    lst <- doc.as[List[Json]]
  } yield lst.map(_.as[TestCase] match {
    case Right(success) => success
    case Left(failure)  => throw failure
  })

  val tests = eitherTests match {
    case Right(success) => success
    case Left(failure)  => throw failure
  }

  val pageHost = "www.snowplowanalytics.com"
  val internalDomains =
    List("www.subdomain1.snowplowanalytics.com", "www.subdomain2.snowplowanalytics.com")

  val resource   = getClass.getResource("/referers.json").getPath
  val ioParser   = CreateParser[IO].create(resource).unsafeRunSync().fold(throw _, identity)
  val evalParser = CreateParser[Eval].create(resource).value.fold(throw _, identity)

  "parse" should {
    s"extract the expected details from referer with spec" in {
      for (test <- tests) yield {
        val expected = Medium.fromString(test.medium) match {
          case Some(UnknownMedium)  => Some(UnknownReferer(UnknownMedium))
          case Some(SearchMedium)   => Some(SearchReferer(SearchMedium, test.source.get, test.term))
          case Some(InternalMedium) => Some(InternalReferer(InternalMedium))
          case Some(SocialMedium)   => Some(SocialReferer(SocialMedium, test.source.get))
          case Some(EmailMedium)    => Some(EmailReferer(EmailMedium, test.source.get))
          case Some(PaidMedium)     => Some(PaidReferer(PaidMedium, test.source.get))
          case _                    => throw new Exception(s"Bad medium: ${test.medium}")
        }
        val ioActual   = ioParser.parse(new URI(test.uri), Some(pageHost), internalDomains)
        val evalActual = evalParser.parse(new URI(test.uri), Some(pageHost), internalDomains)

        expected shouldEqual ioActual
        expected shouldEqual evalActual
      }
    }
  }

}
