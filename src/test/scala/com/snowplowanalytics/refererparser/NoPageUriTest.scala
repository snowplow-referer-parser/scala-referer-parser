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

class NoPageUriTest extends Specification {
  def is = s2"""
  An empty page URI should
    not interfere with the referer parsing $e1 
  No page URI should
    not interfere with the referer parsing $e2 
  A page URI should
    not interfere with the referer parsing $e3 
"""
  val refererUri =
    "http://www.google.com/search?q=gateway+oracle+cards+denise+linn&hl=en&client=safari"
  val expected = Some(
    ExternalReferer("search", "Google", Some("gateway oracle cards denise linn"))
  )

  val resource   = getClass.getResource("/referers.json").getPath
  val ioParser   = CreateParser[IO].create(resource).unsafeRunSync().fold(throw _, identity)
  val evalParser = CreateParser[Eval].create(resource).value.fold(throw _, identity)

  def e1 =
    (ioParser.parse(refererUri, "") must_== expected) and
      (evalParser.parse(refererUri, "") must_== expected)

  def e2 =
    (ioParser.parse(refererUri) must_== expected) and
      (evalParser.parse(refererUri) must_== expected)

  def e3 =
    (ioParser.parse(refererUri) must_== expected) and
      (evalParser.parse(refererUri) must_== expected)
}
