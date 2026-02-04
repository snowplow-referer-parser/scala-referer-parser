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

class CorruptedRefererUriTest extends Specification {
  def is = s2"""
  A corrupted referer URI should
    return None, not throw an Exception $e1
  """

  val resource   = getClass.getResource("/referers.json").getPath
  val ioParser   = CreateParser[IO].createFromFile(resource).unsafeRunSync().fold(throw _, identity)
  val evalParser = CreateParser[Eval].createFromFile(resource).value.fold(throw _, identity)

  def e1 = {
    val refererUri = "http://bigcommerce%20wordpress%20plugin/"
    (ioParser.parse(refererUri) must beNone) and
      (evalParser.parse(refererUri) must beNone)
  }
}
