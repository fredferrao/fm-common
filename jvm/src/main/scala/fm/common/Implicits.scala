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

import fm.common.rich._
import java.io.{File, InputStream}
import java.nio.file.Path
import java.util.Locale

import scala.concurrent.{Await, Future}

import scala.util.Try

object Implicits extends Implicits {
  // Duplicated in both the JVM and JS version of Implicits.scala
  implicit class ToImmutableArrayByte   (val col: TraversableOnce[Byte])    extends AnyVal { def toImmutableArray: ImmutableArray[Byte]    = ImmutableArray.copy(col) }
  implicit class ToImmutableArrayShort  (val col: TraversableOnce[Short])   extends AnyVal { def toImmutableArray: ImmutableArray[Short]   = ImmutableArray.copy(col) }
  implicit class ToImmutableArrayInt    (val col: TraversableOnce[Int])     extends AnyVal { def toImmutableArray: ImmutableArray[Int]     = ImmutableArray.copy(col) }
  implicit class ToImmutableArrayLong   (val col: TraversableOnce[Long])    extends AnyVal { def toImmutableArray: ImmutableArray[Long]    = ImmutableArray.copy(col) }
  implicit class ToImmutableArrayFloat  (val col: TraversableOnce[Float])   extends AnyVal { def toImmutableArray: ImmutableArray[Float]   = ImmutableArray.copy(col) }
  implicit class ToImmutableArrayDouble (val col: TraversableOnce[Double])  extends AnyVal { def toImmutableArray: ImmutableArray[Double]  = ImmutableArray.copy(col) }
  implicit class ToImmutableArrayBoolean(val col: TraversableOnce[Boolean]) extends AnyVal { def toImmutableArray: ImmutableArray[Boolean] = ImmutableArray.copy(col) }
  implicit class ToImmutableArrayChar   (val col: TraversableOnce[Char])    extends AnyVal { def toImmutableArray: ImmutableArray[Char]    = ImmutableArray.copy(col) }
  
  implicit class ToImmutableArrayAnyRef[T <: AnyRef](val col: TraversableOnce[T]) extends AnyVal { def toImmutableArray: ImmutableArray[T] = ImmutableArray.copy[AnyRef](col).asInstanceOf[ImmutableArray[T]] }
}

trait Implicits extends ImplicitsBase {
  implicit def toRichTry[T](t: Try[T]): RichTry[T] = new RichTry(t)
  implicit def toRichFuture[V](f: Future[V]): RichFuture[V] = RichFuture(f)
  implicit def toRichAwait[V](await: Await.type): RichAwait = new RichAwait(await)
  
  implicit def toRichFile(f: File): RichFile = new RichFile(f)
  implicit def toRichJVMString(s: String): RichJVMString = new RichJVMString(s)
  implicit def toRichPath(p: Path): RichPath = new RichPath(p)
  implicit def toRichInputStream(is: InputStream): RichInputStream = new RichInputStream(is)
  
  implicit def toRichLocale(locale: Locale): RichLocale = new RichLocale(locale)
  
  implicit def toRichURL(url: URL): RichURL = new RichURL(url)
  
  implicit def toRichQueryParamsObject(obj: QueryParams.type): RichQueryParams.type = RichQueryParams
  
  implicit def toRichImmutableArray[A](arr: ImmutableArray[A]): RichImmutableArray[A] = new RichImmutableArray(arr)
}