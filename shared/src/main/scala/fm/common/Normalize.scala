/*
 * Copyright 2016 Frugal Mechanic (http://frugalmechanic.com)
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

import fm.common.Implicits._
import java.util.Arrays
import scala.annotation.switch
import scala.collection.mutable.{ArrayBuffer,Builder}

object Normalize {
  def stripAccents(s: String): String = ASCIIUtil.convertToASCII(s)
  
  /**
   * Replaces any non-alphanumeric characters with collapsed spaces
   */
  def lowerAlphanumericWithSpaces(s: String): String = {
    if (null == s) return ""
    
    val sb = new java.lang.StringBuilder
    
    var i: Int = 0
    var prevCh: Char = 0
    while (i < s.length) {
      val ch = stripAccent(s.charAt(i))
      if(Character.isLetterOrDigit(ch)) {
        sb.append(Character.toLowerCase(ch))
        prevCh = ch
      } else if(prevCh != ' ') {
        sb.append(' ')
        prevCh = ' '
      }
      i += 1
    }
    
    sb.toString.trim
  }
  
  /**
   * Removes any non-alphanumeric characters and strips accents (when it can be converted to a single character) - Only allocates a new string if the passed in string is not already normalized
   * 
   * Note: This logic should match reverseLowerAlphanumeric() -- EXCEPT that this implementation now only allocates if it needs to
   */
  def lowerAlphanumeric(s: String): String = lowerAlphanumericWithPositionsImpl(s, false)._1
  
  /**
   * Removes any non-alphanumeric characters and strips accents (when it can be converted to a single character) - Only allocates a new string if the passed in string is not already normalized
   * 
   * Note: This logic should match reverseLowerAlphanumeric() -- EXCEPT that this implementation now only allocates if it needs to
   */
  def lowerAlphanumericWithPositions(s: String): (String, Array[Int]) = lowerAlphanumericWithPositionsImpl(s, true)
  
  /**
   * The implementation for both lowerAlphanumeric and lowerAlphanumericWithPositions
   */
  private def lowerAlphanumericWithPositionsImpl(s: String, includePositions: Boolean): (String, Array[Int]) = {
    if (null == s) return ("", Array())
    
    var arr: Array[Char] = null // The lowerAlphanumeric chars
    var pos: Array[Int] = null // Original positions the lowerAlphanumeric chars came from
    var arrIdx: Int = 0
    
    var i: Int = 0
    while (i < s.length) {
      val ch: Char = stripAccent(s.charAt(i))
      
      if (null == arr && (!Character.isLetterOrDigit(ch) || ch != Character.toLowerCase(ch))) {
        // The original string is not normalized so we need to initialize arr and copy over everything so far
        arr = new Array[Char](s.length)
        if (includePositions) pos = makePositionsArray(s.length, i)
        
        // Copy over everything so far
        if (i > 0) {
          s.getChars(0, i, arr, 0)
          arrIdx = i
        }
      }
      
      // Normal case of building up our new string
      if (null != arr) {
        if (Character.isLetterOrDigit(ch)) {
          arr(arrIdx) = Character.toLowerCase(ch)
          if (includePositions) pos(arrIdx) = i
          arrIdx += 1
        }
      }
      
      i += 1
    }
    
    // If arr is null then the original string is already normalized
    val normalizedString: String = if (null == arr) s else new String(arr, 0, arrIdx)
    
    val normalizedPositions: Array[Int] = if (includePositions) {
      if (null == pos) {
        // If pos is null then arr was null so we just need to fill with 0..normalizedString.length
        makePositionsArray(normalizedString.length, normalizedString.length)
      } else {
        // Otherwise trim the pos array to the same length as the normalized string
        Arrays.copyOf(pos, normalizedString.length)
      }
    } else null
    
    (normalizedString, normalizedPositions)
  }
  
  // Used by lowerAlphanumericWithPositionsImpl
  private def makePositionsArray(length: Int, fillLength: Int = 0): Array[Int] = {
    val arr: Array[Int] = new Array(length)
    
    var i: Int = 0
    while (i < fillLength) {
      arr(i) = i
      i += 1
    }
    
    arr
  }
    
  /**
   * Given the original string and a normalized substring, extract the original version of the normalized substring.
   * e.g. Original: "Foo B.O.S.C.H. Bar"  Normalized: "bosch"  Result: "B.O.S.C.H."
   * 
   * Note: This logic should match lowerAlphanumeric
   */
  def reverseLowerAlphanumeric(original: String, normalized: String): Option[String] = {
    if (original.isBlank || normalized.isBlank) return None
    
    val (normalizedOriginal: String, positions: Array[Int]) = lowerAlphanumericWithPositions(original)
    
    val matchIdx: Int = normalizedOriginal.indexOf(normalized)
    
    if (matchIdx < 0) None else {
      val startIdx: Int = positions(matchIdx)
      
      var endIdx: Int = positions(matchIdx + normalized.length - 1)
      val maxEndIdx: Int = if (matchIdx + normalized.length >= normalizedOriginal.length) original.length else positions(matchIdx + normalized.length)
      
      // Take any additional non-whitespace up to the next normalized character
      while (endIdx < maxEndIdx && !Character.isWhitespace(original.charAt(endIdx))) {
        endIdx += 1
      }
      
      Some(original.substring(startIdx, endIdx))
    }
  }
  
  def lowerAlphaNumericWords(s: String): Array[String] = {
    val buf = new ArrayBuffer[String]
    lowerAlphaNumericWords(s, buf)
    buf.toArray
  }
  
  def lowerAlphaNumericWords(s: String, buf: Builder[String,_]): Unit = {
    val size: Int = s.length
    var i: Int = 0

    var sb = new StringBuilder
    
    while(i < size) {
      val ch: Char = stripAccent(s.charAt(i))
      
      // If its a valid character (alphanumeric or a dot) add it to the StringBuilder
      if (Character.isLetterOrDigit(ch) || ch == '.') {
        sb.append(Character.toLowerCase(ch))
      } else if (sb.length > 0) {
        // Otherwise we have a complete word, add it to the result buffer
        buf += sb.toString
        sb = new StringBuilder
      }
      
      i += 1
    }

    // If there is anything left in the StringBuilder, add it to the result buffer
    if (sb.length > 0) buf += sb.toString
  }

  def stripControl(s: String): String = {
    new String(s.filter{ch => !Character.isISOControl(ch) || '\t' == ch }.toArray)
  }

  def numeric(s: String): String = {
    new String(s.filter{ch => Character.isDigit(ch) || '.' == ch || '-' == ch }.toArray)
  }
  
  /** The word seperator character for urlName */
  private[this] val SepChar: Char = '-'
  
  /** These characters should be transformed into the SepChar in urlName */
  private[this] val ReplaceWithSepChars: Set[Char] = Set('_', '\\', '/', ' ')
  
  /** These characters should be expanded into words in urlName */
  private[this] val ExpandCharMap: Map[Char, String] = Map(
    '&' -> "and",
    '+' -> "plus",
    '"' -> "inch"
  )
  
  /**
   * Transform the string into something that is URL Friendly.
   */
  def urlName(raw: String): String = {
    // 2015-02-19 - This additional step to added to strip accented chars
    val s: String = stripAccents(raw)
    
    val sb = new java.lang.StringBuilder(s.length)
    var i: Int = 0
    var lastCharWasSep: Boolean = false
    
    while (i < s.length) {
      val ch: Char = s.charAt(i)
      
      if (ch == SepChar || ReplaceWithSepChars.contains(ch)) {
        if (!lastCharWasSep) {
          sb.append(SepChar)
          lastCharWasSep = true
        }
      } else if (ExpandCharMap.contains(ch)) {
        if (!lastCharWasSep) sb.append(SepChar)
        sb.append(ExpandCharMap(ch))
        sb.append(SepChar)
        lastCharWasSep = true
      } else if (Character.isLetterOrDigit(ch)) {
        sb.append(Character.toLowerCase(ch))
        lastCharWasSep = false
      }

      i += 1
    }
    
    // We can end up with a leading and/or trailing SepChar so lets remove those
    if (sb.charAt(0) == SepChar) sb.deleteCharAt(0)
    if (sb.charAt(sb.length - 1) == SepChar) sb.deleteCharAt(sb.length - 1)
    
    sb.toString
  }
  
  /**
   * Converts an ASCII Character to it's Unicode Full Width equivalent
   *  
   * scala> val a = (33 to 126).map{ _.toChar }
   * a: scala.collection.immutable.IndexedSeq[Char] = Vector(!, ", #, $, %, &, ', (, ), *, +, ,, -, ., /, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, :, ;, <, =, >, ?, @, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, W, X, Y, Z, [, \, ], ^, _, `, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u, v, w, x, y, z, {, |, }, ~)
   * 
   * scala> val b = (65281 to 65374).map{ _.toChar }
   * b: scala.collection.immutable.IndexedSeq[Char] = Vector(！, ＂, ＃, ＄, ％, ＆, ＇, （, ）, ＊, ＋, ，, －, ．, ／, ０, １, ２, ３, ４, ５, ６, ７, ８, ９, ：, ；, ＜, ＝, ＞, ？, ＠, Ａ, Ｂ, Ｃ, Ｄ, Ｅ, Ｆ, Ｇ, Ｈ, Ｉ, Ｊ, Ｋ, Ｌ, Ｍ, Ｎ, Ｏ, Ｐ, Ｑ, Ｒ, Ｓ, Ｔ, Ｕ, Ｖ, Ｗ, Ｘ, Ｙ, Ｚ, ［, ＼, ］, ＾, ＿, ｀, ａ, ｂ, ｃ, ｄ, ｅ, ｆ, ｇ, ｈ, ｉ, ｊ, ｋ, ｌ, ｍ, ｎ, ｏ, ｐ, ｑ, ｒ, ｓ, ｔ, ｕ, ｖ, ｗ, ｘ, ｙ, ｚ, ｛, ｜, ｝, ～)
   * 
   * scala> (a zip b)
   * res44: scala.collection.immutable.IndexedSeq[(Char, Char)] = Vector((!,！), (",＂), (#,＃), ($,＄), (%,％), (&,＆), (',＇), ((,（), (),）), (*,＊), (+,＋), (,,，), (-,－), (.,．), (/,／), (0,０), (1,１), (2,２), (3,３), (4,４), (5,５), (6,６), (7,７), (8,８), (9,９), (:,：), (;,；), (<,＜), (=,＝), (>,＞), (?,？), (@,＠), (A,Ａ), (B,Ｂ), (C,Ｃ), (D,Ｄ), (E,Ｅ), (F,Ｆ), (G,Ｇ), (H,Ｈ), (I,Ｉ), (J,Ｊ), (K,Ｋ), (L,Ｌ), (M,Ｍ), (N,Ｎ), (O,Ｏ), (P,Ｐ), (Q,Ｑ), (R,Ｒ), (S,Ｓ), (T,Ｔ), (U,Ｕ), (V,Ｖ), (W,Ｗ), (X,Ｘ), (Y,Ｙ), (Z,Ｚ), ([,［), (\,＼), (],］), (^,＾), (_,＿), (`,｀), (a,ａ), (b,ｂ), (c,ｃ), (d,ｄ), (e,ｅ), (f,ｆ), (g,ｇ), (h,ｈ), (i,ｉ), (j,ｊ), (k,ｋ), (l,ｌ), (m,ｍ), (n,ｎ), (o,ｏ), (p,ｐ), (q,ｑ), (r,ｒ), (s,ｓ), (t,ｔ), (u,ｕ), (v,ｖ), (w,ｗ), (x,ｘ), (y,ｙ), (z,ｚ), ({,｛), (|,｜), (},｝), (~,～))
   * 
   */
  def toFullWidth(ch: Char): Char = if (ch == ' ') 12288.toChar else if (ch >= 33 && ch <= 126) (ch+65248).toChar else ch
  
  /**
   * Converts ASCII Characters in a String to their Unicode Full Width equivalent
   */
  def toFullWidth(s: String): String = s.map{ toFullWidth }
  
  /**
   * Converts Accented Characters to the Non-Accented Equivalent.
   * 
   * Note: This only works for when there is a 1 to 1 Character equivalence (i.e. it does not work for stuff like Æ which needs to expand to AE)
   */
  private def stripAccent(c: Char): Char = ASCIIUtil.toASCIIChar(c)
}
