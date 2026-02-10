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
 * Referer - returned from parse, representing any type of referer source. Can be internal, unknown,
 * or external with a specific medium type.
 */
sealed trait Referer

/**
 * Internal referer - traffic from the same domain as the page.
 */
case object InternalReferer extends Referer

/**
 * Unknown referer - traffic from an unrecognized source.
 */
case object UnknownReferer extends Referer

/**
 * External referer - traffic from a known external source with a specific medium. All external
 * referers have a source and may optionally have a term extracted from query parameters.
 */
final case class ExternalReferer(
  medium: String,
  source: String,
  term: Option[String]
) extends Referer
