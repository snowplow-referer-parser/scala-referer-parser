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
  CreateParser.create should
    give precedence to referers from map over referers from file $e1
    use referers from file when referers from map don't match $e2
    support referers from map with search parameters $e3
  Parser.fromMap
    work without file $e4
    return unknown for referers not in map $e5
    support subdomain matching $e6 
    support path-specific referers $e7 
  """

  val resource = getClass.getResource("/referers.json").getPath

  def e1 = {
    // Google is defined in the file as a search engine, but we override it to be social
    val referers = Map(
      "www.google.com" -> RefererLookup("social", "Google Custom", Nil)
    )

    val ioParser = CreateParser[IO]
      .create(resource, referers)
      .unsafeRunSync()
      .fold(throw _, identity)

    val evalParser = CreateParser[Eval]
      .create(resource, referers)
      .value
      .fold(throw _, identity)

    val refererUri = "http://www.google.com/search?q=test"
    val expected   = Some(ExternalReferer("social", "Google Custom", None))

    (expected shouldEqual ioParser.parse(refererUri)) and
      (expected shouldEqual evalParser.parse(refererUri))
  }

  def e2 = {
    val referers = Map(
      "www.example.org" -> RefererLookup("search", "Example Custom", List("q"))
    )

    val ioParser = CreateParser[IO]
      .create(resource, referers)
      .unsafeRunSync()
      .fold(throw _, identity)

    // Yahoo is defined in the file and not in the map
    val yahooUri    = "http://search.yahoo.com/search?p=test"
    val yahooResult = ioParser.parse(yahooUri)

    yahooResult must beSome.like { case ExternalReferer(medium, source, _) =>
      (medium shouldEqual "search") and
        (source shouldEqual "Yahoo!")
    }
  }

  def e3 = {
    val referers = Map(
      "custom.search.com" -> RefererLookup("search", "Custom Search", List("query", "q"))
    )

    val ioParser = CreateParser[IO]
      .create(resource, referers)
      .unsafeRunSync()
      .fold(throw _, identity)

    val refererUri = "http://custom.search.com?query=scala+programming"
    val expected   = Some(ExternalReferer("search", "Custom Search", Some("scala programming")))

    expected shouldEqual ioParser.parse(refererUri)
  }

  def e4 = {
    val referers = Map(
      "custom.example.com" -> RefererLookup("social", "Custom Social", Nil),
      "search.custom.com" -> RefererLookup("search", "Custom Search", List("q"))
    )

    val parser = Parser.fromMap(referers)

    val socialUri      = "http://custom.example.com/page"
    val socialExpected = Some(ExternalReferer("social", "Custom Social", None))

    val searchUri      = "http://search.custom.com?q=test+query"
    val searchExpected = Some(ExternalReferer("search", "Custom Search", Some("test query")))

    (socialExpected shouldEqual parser.parse(socialUri)) and
      (searchExpected shouldEqual parser.parse(searchUri))
  }

  def e5 = {
    val referers = Map(
      "custom.example.com" -> RefererLookup("social", "Custom Social", Nil)
    )

    val parser = Parser.fromMap(referers)

    val googleUri = "http://www.google.com/search?q=test"
    val expected  = Some(UnknownReferer)

    expected shouldEqual parser.parse(googleUri)
  }

  def e6 = {
    val referers = Map(
      "example.com" -> RefererLookup("search", "Example", List("q"))
    )

    val parser = Parser.fromMap(referers)

    val subdomainUri = "http://www.example.com?q=test"
    val expected     = Some(ExternalReferer("search", "Example", Some("test")))

    expected shouldEqual parser.parse(subdomainUri)
  }

  def e7 = {
    val referers = Map(
      "example.com/search" -> RefererLookup("search", "Example Search", List("q")),
      "example.com" -> RefererLookup("social", "Example Social", Nil)
    )

    val parser = Parser.fromMap(referers)

    val searchUri      = "http://example.com/search?q=test"
    val searchExpected = Some(ExternalReferer("search", "Example Search", Some("test")))

    val socialUri      = "http://example.com/other"
    val socialExpected = Some(ExternalReferer("social", "Example Social", None))

    (searchExpected shouldEqual parser.parse(searchUri)) and
      (socialExpected shouldEqual parser.parse(socialUri))
  }
}
