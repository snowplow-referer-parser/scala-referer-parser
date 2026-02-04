/**
 * Copyright 2012-2022 Snowplow Analytics Ltd
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

class ReferersFromMapTest extends Specification {
  def is = s2"""
  createFromFileWithOverrides should
    give precedence to referers from map over referers from file $e1
    use referers from file when referers from map don't match $e2
    support referers from map with search parameters $e3
  createFromMap should
    work without file $e4
    return unknown for referers not in custom map $e5
  Referers from the map should
    support subdomain matching $e6 
    support path-specific referers $e7 
  """

  val resource = getClass.getResource("/referers.json").getPath

  def e1 = {
    // Google is defined in the file as a search engine, but we override it to be social
    val referers = Map(
      "www.google.com" -> RefererLookup(SocialMedium, "Google Custom", Nil)
    )

    val ioParser = CreateParser[IO]
      .createFromFileWithOverrides(resource, referers)
      .unsafeRunSync()
      .fold(throw _, identity)

    val evalParser = CreateParser[Eval]
      .createFromFileWithOverrides(resource, referers)
      .value
      .fold(throw _, identity)

    val refererUri = "http://www.google.com/search?q=test"
    val expected   = Some(SocialReferer(SocialMedium, "Google Custom"))

    // Custom referer should override file referer
    (expected shouldEqual ioParser.parse(refererUri)) and
      (expected shouldEqual evalParser.parse(refererUri))
  }

  def e2 = {
    val referers = Map(
      "www.example.org" -> RefererLookup(SearchMedium, "Example Custom", List("q"))
    )

    val ioParser = CreateParser[IO]
      .createFromFileWithOverrides(resource, referers)
      .unsafeRunSync()
      .fold(throw _, identity)

    // Yahoo is in the file but not in custom referers, should use file definition
    val yahooUri    = "http://search.yahoo.com/search?p=test"
    val yahooResult = ioParser.parse(yahooUri)

    yahooResult must beSome.which { referer =>
      (referer must beAnInstanceOf[SearchReferer]) and
        (referer.asInstanceOf[SearchReferer].source shouldEqual "Yahoo!")
    }
  }

  def e3 = {
    val referers = Map(
      "custom.search.com" -> RefererLookup(SearchMedium, "Custom Search", List("query", "q"))
    )

    val ioParser = CreateParser[IO]
      .createFromFileWithOverrides(resource, referers)
      .unsafeRunSync()
      .fold(throw _, identity)

    val refererUri = "http://custom.search.com?query=scala+programming"
    val expected   = Some(SearchReferer(SearchMedium, "Custom Search", Some("scala programming")))

    expected shouldEqual ioParser.parse(refererUri)
  }

  def e4 = {
    val referers = Map(
      "custom.example.com" -> RefererLookup(SocialMedium, "Custom Social", Nil),
      "search.custom.com" -> RefererLookup(SearchMedium, "Custom Search", List("q"))
    )

    val ioParser = CreateParser[IO]
      .createFromMap(referers)
      .unsafeRunSync()
      .fold(throw _, identity)

    val evalParser = CreateParser[Eval]
      .createFromMap(referers)
      .value
      .fold(throw _, identity)

    // Should find referer
    val socialUri      = "http://custom.example.com/page"
    val socialExpected = Some(SocialReferer(SocialMedium, "Custom Social"))

    // Should find search referer with term
    val searchUri      = "http://search.custom.com?q=test+query"
    val searchExpected = Some(SearchReferer(SearchMedium, "Custom Search", Some("test query")))

    (socialExpected shouldEqual ioParser.parse(socialUri)) and
      (socialExpected shouldEqual evalParser.parse(socialUri)) and
      (searchExpected shouldEqual ioParser.parse(searchUri)) and
      (searchExpected shouldEqual evalParser.parse(searchUri))
  }

  def e5 = {
    val referers = Map(
      "custom.example.com" -> RefererLookup(SocialMedium, "Custom Social", Nil)
    )

    val ioParser = CreateParser[IO]
      .createFromMap(referers)
      .unsafeRunSync()
      .fold(throw _, identity)

    // Google is not in custom map (and we're not loading the file), should be unknown
    val googleUri = "http://www.google.com/search?q=test"
    val expected  = Some(UnknownReferer(UnknownMedium))

    expected shouldEqual ioParser.parse(googleUri)
  }

  def e6 = {
    val referers = Map(
      "example.com" -> RefererLookup(SearchMedium, "Example", List("q"))
    )

    val ioParser = CreateParser[IO]
      .createFromMap(referers)
      .unsafeRunSync()
      .fold(throw _, identity)

    // Should match subdomain
    val subdomainUri = "http://www.example.com?q=test"
    val expected     = Some(SearchReferer(SearchMedium, "Example", Some("test")))

    expected shouldEqual ioParser.parse(subdomainUri)
  }

  def e7 = {
    val referers = Map(
      "example.com/search" -> RefererLookup(SearchMedium, "Example Search", List("q")),
      "example.com" -> RefererLookup(SocialMedium, "Example Social", Nil)
    )

    val ioParser = CreateParser[IO]
      .createFromMap(referers)
      .unsafeRunSync()
      .fold(throw _, identity)

    // Should match path-specific referer
    val searchUri      = "http://example.com/search?q=test"
    val searchExpected = Some(SearchReferer(SearchMedium, "Example Search", Some("test")))

    // Should match domain-only referer
    val socialUri      = "http://example.com/other"
    val socialExpected = Some(SocialReferer(SocialMedium, "Example Social"))

    (searchExpected shouldEqual ioParser.parse(searchUri)) and
      (socialExpected shouldEqual ioParser.parse(socialUri))
  }
}
