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

import java.net.URI

import cats.Eval
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.specs2.Specification

class ParseArgTypesTest extends Specification {
  def is = s2"""
  parse should
    work the same regardless of which argument types are used to call it $e1 
  """

  val resource   = getClass.getResource("/referers.json").getPath
  val ioParser   = CreateParser[IO].create(resource).unsafeRunSync().fold(throw _, identity)
  val evalParser = CreateParser[Eval].create(resource).value.fold(throw _, identity)

  def e1 = {
    val refererUri = "http://www.psychicbazaar.com/catalog/pendula"
    val refererURI = new URI(refererUri)
    val pageURI =
      new URI("http://www.psychicbazaar.com/catalog/pendula/lo-scarabeo-silver-cone-pendulum")
    val pageHost = pageURI.getHost
    val expected = Some(InternalReferer)
    (ioParser.parse(refererUri, pageHost) must_== expected) and
      (ioParser.parse(refererUri, pageURI) must_== expected) and
      (ioParser.parse(refererURI, pageHost) must_== expected) and
      (ioParser.parse(refererURI, pageURI) must_== expected) and
      (evalParser.parse(refererUri, pageHost) must_== expected) and
      (evalParser.parse(refererUri, pageURI) must_== expected) and
      (evalParser.parse(refererURI, pageHost) must_== expected) and
      (evalParser.parse(refererURI, pageURI) must_== expected)
  }
}
