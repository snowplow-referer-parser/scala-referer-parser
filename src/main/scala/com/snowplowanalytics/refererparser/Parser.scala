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

import java.net.{URI, URLDecoder}

import scala.io.Source

import cats.{Eval, Id}
import cats.effect.Sync
import cats.syntax.either._
import cats.syntax.functor._
import scala.collection.compat.immutable.LazyList

trait CreateParser[F[_]] {
  def create(filePath: String, referers: Map[String, RefererLookup] = Map.empty): F[Either[Exception, Parser]]
}

object CreateParser {
  def apply[F[_]](implicit ev: CreateParser[F]): CreateParser[F] = ev

  implicit def syncCreateParser[F[_]: Sync]: CreateParser[F] =
    new CreateParser[F] {
      def create(filePath: String, referers: Map[String, RefererLookup]): F[Either[Exception, Parser]] =
        Sync[F]
          .blocking(Source.fromFile(filePath).mkString)
          .map(rawJson => ParseReferers.loadJsonFromString(rawJson))
          .map(_.map(fileReferers => new Parser(fileReferers, referers)))
    }

  implicit def evalCreateParser: CreateParser[Eval] =
    new CreateParser[Eval] {
      def create(filePath: String, referers: Map[String, RefererLookup]): Eval[Either[Exception, Parser]] =
        Eval
          .later(Source.fromFile(filePath).mkString)
          .map(rawJson => ParseReferers.loadJsonFromString(rawJson))
          .map(_.map(fileReferers => new Parser(fileReferers, referers)))
    }

  implicit def idCreateParser: CreateParser[Id] =
    new CreateParser[Id] {
      def create(filePath: String, referers: Map[String, RefererLookup]): Either[Exception, Parser] = {
        val rawJson = Source.fromFile(filePath).mkString
        ParseReferers
          .loadJsonFromString(rawJson)
          .map(fileReferers => new Parser(fileReferers, referers))
      }
    }
}

class Parser private[refererparser] (
  referersFromFile: Map[String, RefererLookup],
  referersFromMap: Map[String, RefererLookup]
) {

  private def toUri(uri: String): Option[URI] =
    if (uri == "")
      None
    else
      Either.catchNonFatal(new URI(uri)).toOption

  def parse(refererUri: URI): Option[Referer] =
    parse(refererUri, None, Nil)

  def parse(refererUri: String): Option[Referer] =
    toUri(refererUri).flatMap(uri => parse(uri, None, Nil))

  def parse(refererUri: URI, pageHost: String): Option[Referer] =
    parse(refererUri, Some(pageHost), Nil)

  def parse(refererUri: String, pageHost: String): Option[Referer] =
    toUri(refererUri).flatMap(uri => parse(uri, Some(pageHost), Nil))

  def parse(refererUri: URI, pageUri: URI): Option[Referer] =
    parse(refererUri, Some(pageUri.getHost), Nil)

  def parse(refererUri: String, pageUri: URI): Option[Referer] =
    toUri(refererUri).flatMap(uri => parse(uri, Some(pageUri.getHost), Nil))

  /** Parses a `refererUri` URI to return either Some Referer, or None. */
  def parse(
    refererUri: URI,
    pageHost: Option[String],
    internalDomains: List[String]
  ): Option[Referer] = {
    val scheme = refererUri.getScheme
    val host   = refererUri.getHost
    val path   = refererUri.getPath
    val query  = Option(refererUri.getRawQuery)

    val validSchemes = Seq("http", "https", "android-app")

    val validUri = validSchemes.contains(scheme) && host != null && path != null

    if (validUri)
      if ( // Check for internal domains
        pageHost.exists(_.equals(host)) ||
        internalDomains.map(_.trim()).contains(host)
      )
        Some(InternalReferer(InternalMedium))
      else
        Some(
          lookupReferer(host, path)
            .map { lookup =>
              lookup.medium match {
                case UnknownMedium => UnknownReferer(UnknownMedium)
                case SearchMedium =>
                  SearchReferer(
                    SearchMedium,
                    lookup.source,
                    query.flatMap(q => extractSearchTerm(q, lookup.parameters))
                  )
                case InternalMedium => InternalReferer(InternalMedium)
                case SocialMedium   => SocialReferer(SocialMedium, lookup.source)
                case EmailMedium    => EmailReferer(EmailMedium, lookup.source)
                case PaidMedium     => PaidReferer(PaidMedium, lookup.source)
                case ChatbotMedium  => ChatbotReferer(ChatbotMedium, lookup.source)
              }
            }
            .getOrElse(UnknownReferer(UnknownMedium))
        )
    else
      None
  }

  private def extractSearchTerm(query: String, possibleParameters: List[String]): Option[String] =
    extractQueryParams(query).find(p => possibleParameters.contains(p._1)).map(_._2)

  private def extractQueryParams(query: String): List[(String, String)] =
    query.split("&").toList.map { pair =>
      val equalsIndex = pair.indexOf("=")
      if (equalsIndex > 0)
        (
          decodeUriPart(pair.substring(0, equalsIndex)),
          decodeUriPart(pair.substring(equalsIndex + 1))
        )
      else
        (decodeUriPart(pair), "")
    }

  private def decodeUriPart(part: String): String = URLDecoder.decode(part, "UTF-8")

  private def lookupReferer(refererHost: String, refererPath: String): Option[RefererLookup] = {
    val hosts = hostsToTry(refererHost).to(LazyList)
    val paths = pathsToTry(refererPath).to(LazyList)

    // Since streams are lazy we don't calculate past the first element
    val resultsFromMap: LazyList[RefererLookup] = for {
      path <- paths
      host <- hosts
      result <- referersFromMap.get(host + path).to(LazyList)
    } yield result

    resultsFromMap.headOption.orElse {
      val results: LazyList[RefererLookup] = for {
        path <- paths
        host <- hosts
        result <- referersFromFile.get(host + path).to(LazyList)
      } yield result

      results.headOption
    }
  }

  /**
   * Splits a full hostname into possible hosts to lookup. For instance,
   * hostsToTry("www.google.com") == List("www.google.com", "google.com", "com")
   */
  private def hostsToTry(refererHost: String): List[String] =
    refererHost
      .split("\\.")
      .toList
      .scanRight("")((part, full) => s"$part.$full")
      .init
      .map(s => s.substring(0, s.length - 1))

  /**
   * Splits a full path into possible paths to try. Includes full path, no path and first path
   * level. For instance, pathsToTry("google.com/images/1/2/3") == List("/images/1/2/3", "/images",
   * "")
   */
  private def pathsToTry(refererPath: String): List[String] =
    refererPath.split("/").find(_ != "") match {
      case Some(p) => List(refererPath, "/" + p, "")
      case None    => List("")
    }
}

object Parser {
  def fromMap(referers: Map[String, RefererLookup]): Parser =
    new Parser(Map.empty, referers)
}
