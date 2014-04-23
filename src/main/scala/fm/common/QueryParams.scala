/*
 * Copyright 2014 Frugal Mechanic (http://frugalmechanic.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fm.common

import java.net.{URLDecoder,URLEncoder}
import scala.collection.SeqLike
import scala.collection.mutable.Builder
import Implicits._

object QueryParams {
  private def defaultCharset: String = "UTF-8"
  
  /** Create a QueryParams instance given a URL or Query String */
  def apply(queryString: String): QueryParams = apply(queryString, defaultCharset)
  
  /** Create Query Params form a URI */
  def apply(uri: URI): QueryParams = apply(uri, defaultCharset)
  
  /** Create Query Params form a URL */
  def apply(url: URL): QueryParams = apply(url, defaultCharset)
  
  /** Create Query Params form a URI */
  def apply(uri: URI, charset: String): QueryParams = apply(uri.getRawQuery(), charset)
  
  /** Create Query Params form a URL */
  def apply(url: URL, charset: String): QueryParams = apply(url.getQuery(), charset)

  /**
   * Create a QueryParams instance given a URL or Query String
   * 
   * @param queryString the URI/URL or Query String to extract Query Parameters from
   */
  def apply(queryString: String, charset: String): QueryParams = {
    if (queryString.isBlank) return empty

    val questionIdx: Int = queryString.indexOf('?')   
    val hashIdx: Int = queryString.indexOf('#')
    
    // Ignore anything before the "?" and after the "#"
    val fixedQueryString: String = if (questionIdx >= 0 || hashIdx >= 0) {
      val question: Int = if (questionIdx >= 0) questionIdx+1 else 0
      val hash: Int = if (hashIdx >= 0) hashIdx else queryString.length
      queryString.substring(question, hash)
    } else queryString
    
    // Should support ";" in addition to "&" per: http://www.w3.org/TR/1999/REC-html401-19991224/appendix/notes.html#h-B.2.2
    val rawParams: Array[String] = fixedQueryString.replace(';','&').split('&')
    
    val params: Seq[(String, String)] = rawParams.map { param: String =>
      val idx: Int = param.indexOf('=')
      if (idx < 0) {
        (URLDecoder.decode(param, charset), null)
      } else {
        (URLDecoder.decode(param.substring(0, idx), charset), URLDecoder.decode(param.substring(idx + 1), charset))
      }
    }
    
    apply(params)
  }
  
  def apply(params: Map[String, Seq[String]]): QueryParams = apply(params.toSeq.flatMap{ case (k, vals) => vals.map{ v => (k, v) } })
  
  def apply(head: (String, String), rest: (String,String)*): QueryParams = new QueryParams(head +: rest)
  
  def apply(params: Seq[(String, String)]): QueryParams = new QueryParams(params)
  
  /** An empty instance of QueryParams */
  val empty: QueryParams = new QueryParams()
  
  def newBuilder: Builder[(String, String), QueryParams] = new QueryParamsBuilder
}

final class QueryParamsBuilder extends Builder[(String, String), QueryParams] {
  private[this] val builder = Vector.newBuilder[(String, String)]
  def +=(param: (String, String)): this.type = { builder += param; this }
  def result(): QueryParams = QueryParams(builder.result)
  def clear(): Unit = builder.clear()
}

/**
 * Represents immutable query parameters from a query string (e.g. "?foo=bar&asd=qwe").
 * 
 * This class distinguishes between 3 different types of values for a key:
 *   - null - "?foo"
 *   - blank - "?foo="
 *   - non-blank - "?foo=bar"
 */
final class QueryParams private (params: Seq[(String, String)] = Nil) extends Seq[(String, String)] with SeqLike[(String, String), QueryParams] {
  
  /**
   * Optionally Returns the first non-null value for the given key 
   */
  def getFirst(key: String): Option[String] = nonNullValuesForKey(key).headOption
  
  /**
   * Optionally Returns the first non-blank value for the given key 
   */
  def getFirstNonBlank(key: String): Option[String] = nonBlankValuesForKey(key).headOption
  
  /**
   * Returns the first non-null value for the given key or throws a NoSuchElementException if none exists.
   */
  def first(key: String): String = getFirst(key).getOrElse{ throw new NoSuchElementException(key) }
  
  /**
   * Returns the first non-blank value for the given key or throws a NoSuchElementException if none exists.
   */
  def firstNonBlank(key: String): String = getFirstNonBlank(key).getOrElse{ throw new NoSuchElementException(key) }
  
  /**
   * Returns all values for the given key.  An Empty Seq is returns if the key doesn't exist or there are no non-null values.
   */
  def get(key: String): Seq[String] = nonNullValuesForKey(key)
  
  /**
   * Returns all non-blank values for the given key.  An Empty Seq is returns if the key doesn't exist or there are no non-null values.
   */
  def getNonBlank(key: String): Seq[String] = nonBlankValuesForKey(key)

  /**
   * Returns all values for the given key or throws a NoSuchElementException if the key doesn't exists. An Empty Seq is returned
   * if the key exists but has no non-null values.
   */
  def apply(key: String): Seq[String] = {
    val vals: Seq[String] = get(key)
    if (vals.nonEmpty || contains(key)) vals else throw new NoSuchElementException(key)
  }
  
  /**
   * Returns all non-null values for the given key or throws a NoSuchElementException if the key doesn't have non-null values.
   */
  def nonNull(key: String): Seq[String] = {
    val vals: Seq[String] = nonNullValuesForKey(key)
    if (vals.nonEmpty) vals else throw new NoSuchElementException(key)
  }
  
  /**
   * Returns all non-blank values for the given key or throws a NoSuchElementException if the key doesn't have non-null values.
   */
  def nonBlank(key: String): Seq[String] = {
    val vals: Seq[String] = nonBlankValuesForKey(key)
    if (vals.nonEmpty) vals else throw new NoSuchElementException(key)
  }
  
  /**
   * Check for key existence
   */
  def contains(key: String): Boolean = hasKey(key)
  
  /**
   * Check for key/value pair existence
   */
  def contains(elem: (String, String)): Boolean = exists (_ == elem)
  
  /**
   * Returns true if there is a matching key (which doesn't need to have a value)
   */
  def hasKey(key: String): Boolean = allKeys.contains(key)
  
  /**
   * Returns true if there is a matching key (which also has a value)
   * 
   * NOTE: The value can be blank (e.g. "?foo=" is blank vs "?foo" is null)
   */
  def hasKeyWithValue(key: String): Boolean = keysWithValues.contains(key)
  
  /**
   * Returns true if there is a matching key (which also has a non-blank value)
   */
  def hasKeyWithNonBlankValue(key: String): Boolean = keysWithNonBlankValues.contains(key)
  
  /**
   * Make sure the key (without a value) exists
   */
  def updated(key: String): QueryParams = updated(key, null)
  
  /**
   * If the key doesn't exist then add it, otherwise replace the first occurance
   * of the key with the new value and remove any other values.
   */
  def updated(key: String, value: String): QueryParams = {
    var found: Boolean = false
    val newParams: Seq[(String, String)] = params.flatMap{ case (k,v) =>      
      if (key == k) {
        if (!found) {
          found = true
          Some((k, value))
        } else None
      } else Some((k,v))
    }
    if (!found) new QueryParams(newParams :+ (key -> value)) else new QueryParams(newParams)
  }
  
  /**
   * Update multiple key/value pairs
   */
  def updated(kvPairs: (String, String)*): QueryParams = {
    var tmp = this
    kvPairs.foreach{ case (k,v) => tmp = tmp.updated(k, v) }
    tmp
  }
  
  /**
   * Add a key/value pair
   * 
   * @return A new QueryParams instance with the added key/value pair
   */
  def add(key: String, value: String): QueryParams = new QueryParams(params ++ Seq(key -> value))
  
  /**
   * Add multiple key/value pairs
   * 
   * @return A new QueryParams instance with the added key/value pair
   */
  def add(kvPairs: (String, String)*): QueryParams = new QueryParams(params ++ kvPairs)
  
  /**
   * Remove any params with blank values
   * 
   * @return A new QueryParams instance without blank values
   */
  def withoutBlankValues(): QueryParams = filter{ case (k, v) => v.isNotBlank }
  
  /**
   * Remove a key/value pair based on the key
   * 
   * @return A new QueryParams instance without the keys
   */
  def remove(keys: String*): QueryParams = filterNot{ case (k, v) => keys.contains(k) }
  
  /**
   * Replaces the entry for the given key only if it was previously mapped to some value.
   */
  def replace(key: String, value: String): QueryParams = if (hasKey(key)) updated(key, value) else this
  
  private def nonNullValuesForKey(key: String): Seq[String] = params.filter{ case (k, v) => k == key && v != null }.map{ case (k, v) => v }.toList
  private def nonBlankValuesForKey(key: String): Seq[String] = params.filter{ case (k, v) => k == key && v.isNotBlank }.map{ case (k, v) => v }.toList
  
  /**
   * The unique set of keys with or without values
   */
  def allKeys: Set[String] = params.map{ case (k, v) => k }.toSet
  
  /**
   * The unique set of keys with non-null values (blank is a valid value)
   */
  def keysWithValues: Set[String] = params.filterNot{ case (k, v) => null == v }.map{ case (k, v) => k }.toSet
  
  /**
   * The unique set of keys with non-blank values
   */
  def keysWithNonBlankValues: Set[String] = params.filterNot{ case (k, v) => v.isBlank }.map{ case (k, v) => k }.toSet
  
  /**
   * The unique set of keys without values
   */
  def keysWithoutValues: Set[String] = params.filter{ case (k, v) => null == v }.map{ case (k, v) => k }.toSet
  
  /**
   * Convert to a Map[String, Seq[String]]
   */
  def toMap: Map[String, Seq[String]] = params.groupBy{ case (k, v) => k }.mapValuesStrict{ values: Seq[(String, String)] => values.map{ _._2 }.filterNot{ _ == null } }
  
  /**
   * Convert all keys to lower case
   */
  def withLowerKeys: QueryParams = new QueryParams(params.map { case (k, v) => (k.toLowerCase, v) })
  
  def prettyString: String = {
    "{"+toMap.toSeq.map{ case (key, values) =>
      key+"=["+values.mkString(",")+"]"
    }.mkString(", ")+"}"
  }
  
  /** An alias for toQueryString("UTF-8") */
  override def toString(): String = toString(QueryParams.defaultCharset)
  
  /** An alias for toQueryString */
  def toString(charset: String): String = toQueryString(charset)
  
  /** An alias for toQueryString("UTF-8") */
  def toQueryString(): String = toQueryString(QueryParams.defaultCharset)
  
  /**
   * Create a valid query string using the current parameters (everything after the ?).
   * 
   * e.g.: foo=bar&hello=world
   */
  def toQueryString(charset: String): String = {
    params.map{ case (k, v) =>
      URLEncoder.encode(k, charset) + (if (null == v) "" else "="+URLEncoder.encode(v, charset))
    }.mkString("&")
  }
  
  //
  // SeqLike Implementation:
  //
  def iterator: Iterator[(String, String)] = params.iterator
  def apply(idx: Int): (String, String) = params(idx)
  def length: Int = params.length
  protected[this] override def newBuilder: Builder[(String, String), QueryParams] = new QueryParamsBuilder
}
