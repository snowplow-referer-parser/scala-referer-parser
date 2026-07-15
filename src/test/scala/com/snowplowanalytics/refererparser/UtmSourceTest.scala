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

import org.specs2.Specification

class UtmSourceTest extends Specification {
  def is = s2"""
  parseUtmSource should
    classify a known utm_source value into medium and source $e1
    return None for an unknown utm_source value $e2
    give precedence to referers from map over referers from file $e3
    load utm_sources from the referer database JSON $e4
    return None when no utm_sources are configured $e5
    capture utm_sources even for an entry with no domains $e6
  """

  private val chatGPT = RefererLookup("chatbot", "ChatGPT", Nil)

  def e1 = {
    val parser = Parser.fromReferers(ParsedReferers(Map.empty, Map("chatgpt.com" -> chatGPT)))

    parser.parseUtmSource("chatgpt.com") shouldEqual Some(ExternalReferer("chatbot", "ChatGPT", None))
  }

  def e2 = {
    val parser = Parser.fromReferers(ParsedReferers(Map.empty, Map("chatgpt.com" -> chatGPT)))

    parser.parseUtmSource("unknown.example.com") shouldEqual None
  }

  def e3 = {
    // The referers-utm.json file classifies "chatgpt.com" as ChatGPT; the map overrides it
    val resource  = getClass.getResource("/referers-utm.json").getPath
    val overrides = ParsedReferers(Map.empty, Map("chatgpt.com" -> RefererLookup("chatbot", "Overridden", Nil)))
    val parser    = CreateParser[cats.Id].create(resource, overrides).fold(throw _, identity)

    parser.parseUtmSource("chatgpt.com") shouldEqual Some(ExternalReferer("chatbot", "Overridden", None))
  }

  def e4 = {
    val resource = getClass.getResource("/referers-utm.json").getPath
    val parser   = CreateParser[cats.Id].create(resource).fold(throw _, identity)

    (parser.parseUtmSource("chatgpt.com") shouldEqual Some(ExternalReferer("chatbot", "ChatGPT", None))) and
      (parser.parseUtmSource("perplexity.ai") shouldEqual Some(ExternalReferer("chatbot", "Perplexity", None)))
  }

  def e5 = {
    // fromMap builds only a domain index, so there are no utm_source entries to match
    val parser = Parser.fromMap(Map("www.google.com" -> RefererLookup("search", "Google", List("q"))))

    parser.parseUtmSource("www.google.com") shouldEqual None
  }

  def e6 = {
    // "NoDomains" in the fixture has an empty domains list but a utm_sources entry; because the two
    // indexes are built independently, its utm_source is still resolvable.
    val resource = getClass.getResource("/referers-utm.json").getPath
    val parser   = CreateParser[cats.Id].create(resource).fold(throw _, identity)

    parser.parseUtmSource("nodomains.example") shouldEqual Some(ExternalReferer("chatbot", "NoDomains", None))
  }
}
