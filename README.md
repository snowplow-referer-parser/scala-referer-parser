# referer-parser Scala library

[![Build Status][ci-image]][ci]
[![Maven Central][release-image]][releases]
[![Coverage Status][coveralls-image]][coveralls]
[![Gitter][chat-image]][chat]

This is the Scala implementation of [referer-parser][referer-parser], the library for extracting attribution data from referer _(sic)_ URLs.

The implementation uses a JSON version of the shared 'database' of known referers found in [`referers.yml`][referers-yml].

The Scala implementation is a core component of [Snowplow][snowplow], the open-source web-scale analytics platform.

### Installation

You can add the following to your SBT config:

```scala
val refererParser = "com.snowplowanalytics" %% "scala-referer-parser" % "4.0.0"
```

### Usage

You can provide wrappers for effects, such as `Sync`, `Eval` or `Id` from [cats-effect][cats-effect]. In the examples below we use `IO`.

```scala
import com.snowplowanalytics.refererparser._
import cats.effect.IO
import cats.data.EitherT
import java.net.URI

val refererUrl = "http://www.google.com/search?q=gateway+oracle+cards+denise+linn&hl=en&client=safari"
val pageUrl    = "http:/www.psychicbazaar.com/shop" // Our current URL

val referersJsonPath = "/opt/referers/referers.json"

// We use EitherT to handle exceptions. The IO routine will short circuit if an exception is returned.
val io: EitherT[IO, Exception, Unit] = for {
  // We can instantiate a new Parser instance with Parser.create
  parser <- EitherT(CreateParser[IO].create(referersJsonPath))

  // Referer is a sealed hierarchy of different referer types
  referer1 <- EitherT.fromOption[IO](parser.parse(refererUrl, pageUrl),
    new Exception("No parseable referer"))
  _ <- EitherT.right(IO { println(referer1) })
    // => ExternalReferer("search","Google",Some("gateway oracle cards denise linn"))

  // You can provide a list of domains which should be considered internal
  referer2 <- EitherT.fromOption[IO](parser.parse(
      new URI("http://www.subdomain1.snowplowanalytics.com"),
      Some("http://www.snowplowanalytics.com"),
      List("www.subdomain1.snowplowanalytics.com", "www.subdomain2.snowplowanalytics.com")
    ), new Exception("No parseable referer"))
  _ <- EitherT.right(IO { println(referer2) })
    // => InternalReferer

  // Various overloads are available for common cases, for instance
  maybeReferer1 = parser.parse("https://www.bing.com/search?q=snowplow")
  maybeReferer2 = parser.parse(new URI("https://www.bing.com/search?q=snowplow"), None, Nil)
  _ <- EitherT.right(IO { println( maybeReferer1 == maybeReferer2 ) }) // => true
} yield Unit

io.value.unsafeRunSync()
```

All known referers are returned as `ExternalReferer(medium, source, term)` where `medium` is a plain `String` (e.g. `"search"`, `"social"`, `"email"`). Traffic from the same domain is `InternalReferer`, and unrecognised sources are `UnknownReferer`.

More examples can be seen in [ParseTest.scala][parsetest-scala]. See [Parser.scala][parser-scala] for all overloads.

#### Classifying an injected `utm_source`

Some sites advertise themselves through a `utm_source` query parameter rather than (or in addition to) a `Referer` header. `parser.parseUtmSource` classifies such a value against the `utm_sources` entries in the referers database, without looking at any referer URI:

```scala
parser.parseUtmSource("chatgpt.com")
  // => Some(ExternalReferer("chatbot", "ChatGPT", None))

parser.parseUtmSource("unknown.example")
  // => None
```

The value is `None` for the `term`. To feed the lookup, add a `utm_sources` list alongside `domains` in the referers JSON:

```json
{
  "chatbot": {
    "ChatGPT": {
      "domains": ["chatgpt.com", "chat.openai.com"],
      "utm_sources": ["chatgpt.com"]
    }
  }
}
```

`domains` and `utm_sources` are indexed independently, so a source's `utm_sources` are recognised even when it has no `domains`.

#### Custom referers

You can augment or override the built-in referers database by passing a `ParsedReferers` to `CreateParser.create`. Entries you provide take precedence over entries from the file.

`ParsedReferers` holds two independent indexes: `byDomain` (referer host/path -> referer, used by `parse`) and `byUtmSource` (`utm_source` value -> referer, used by `parseUtmSource`). If you only need domain-based lookups, wrap a map with `ParsedReferers.fromDomainMap`:

```scala
import com.snowplowanalytics.refererparser._
import cats.effect.IO

val customReferers = ParsedReferers.fromDomainMap(Map(
  "custom.search.com" -> RefererLookup("search", "Custom Search", List("q")),
  "www.google.com"    -> RefererLookup("social", "Google Custom", Nil) // overrides built-in
))

val parser = CreateParser[IO]
  .create("/opt/referers/referers.json", customReferers)
  .unsafeRunSync()
  .fold(throw _, identity)
```

To supply `utm_source` entries too, construct `ParsedReferers` directly:

```scala
val customReferers = ParsedReferers(
  byDomain    = Map("chatgpt.com" -> RefererLookup("chatbot", "ChatGPT", Nil)),
  byUtmSource = Map("chatgpt.com" -> RefererLookup("chatbot", "ChatGPT", Nil))
)
```

If you don't need a referers file at all, use `Parser.fromMap` (domain lookups only) or `Parser.fromReferers` (to also provide `utm_sources`):

```scala
val parser = Parser.fromMap(Map(
  "example.com" -> RefererLookup("search", "Example", List("q"))
))

val parserWithUtm = Parser.fromReferers(ParsedReferers(
  byDomain    = Map("example.com" -> RefererLookup("search", "Example", List("q"))),
  byUtmSource = Map("example.com" -> RefererLookup("search", "Example", Nil))
))
```

#### Parsing a `Json` document directly

`ParseReferers.loadJson` is public and lets you build a `ParsedReferers` from an already-parsed Circe `Json` document:

```scala
import com.snowplowanalytics.refererparser.{ParseReferers, ParsedReferers}
import io.circe.parser.parse

val json = parse("""{"search":{"Google":{"domains":["google.com"],"parameters":["q"]}}}""").toOption.get
val referers: ParsedReferers = ParseReferers.loadJson(json).fold(throw _, identity)
```

[parsetest-scala]: src/test/scala/com/snowplowanalytics/refererparser/ParseTest.scala
[parser-scala]: src/main/scala/com/snowplowanalytics/refererparser/Parser.scala

## Contributing

Check out [our contributing guide](CONTRIBUTING.md).

## Copyright and license

The referer-parser Java/Scala library is copyright 2012-present Snowplow Analytics Ltd.

Licensed under the [Apache License, Version 2.0][license] (the "License");
you may not use this software except in compliance with the License.

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

[snowplow]: https://github.com/snowplow/snowplow

[referer-parser]: https://github.com/snowplow-referer-parser/referer-parser
[referers-yml]: https://github.com/snowplow-referer-parser/referer-parser/blob/develop/resources/referers.yml

[cats-effect]: https://github.com/typelevel/cats-effect

[license]: http://www.apache.org/licenses/LICENSE-2.0

[ci]: https://github.com/snowplow-referer-parser/scala-referer-parser/actions?query=workflow%3ACI
[ci-image]: https://github.com/snowplow-referer-parser/scala-referer-parser/workflows/CI/badge.svg

[releases]: https://maven-badges.herokuapp.com/maven-central/com.snowplowanalytics/scala-referer-parser_2.13
[release-image]: https://maven-badges.herokuapp.com/maven-central/com.snowplowanalytics/scala-referer-parser_2.13/badge.svg

[coveralls]: https://coveralls.io/github/snowplow-referer-parser/scala-referer-parser?branch=master
[coveralls-image]: https://coveralls.io/repos/github/snowplow-referer-parser/scala-referer-parser/badge.svg?branch=master

[chat]: https://gitter.im/snowplow-referer-parser/scala-referer-parser?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=body_badge
[chat-image]: https://badges.gitter.im/snowplow-referer-parser/scala-referer-parser.svg
