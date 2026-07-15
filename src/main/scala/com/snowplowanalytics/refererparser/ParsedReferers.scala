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

/**
 * A parsed referer database, split into two independent lookup indexes:
 *   - `byDomain`: referer host (+ optional path) -> referer, used when parsing a Referer URI
 *   - `byUtmSource`: injected `utm_source` value -> referer, used to classify query parameters
 *
 * The two are built directly from the referer entries rather than one being derived from the other,
 * so `utm_sources` are never tied to the presence of `domains`.
 */
final case class ParsedReferers(
  byDomain: Map[String, RefererLookup],
  byUtmSource: Map[String, RefererLookup]
)

object ParsedReferers {
  val empty: ParsedReferers = ParsedReferers(Map.empty, Map.empty)

  /** Build from a domain-keyed map only (no utm_source entries). */
  def fromDomainMap(byDomain: Map[String, RefererLookup]): ParsedReferers =
    ParsedReferers(byDomain, Map.empty)
}
