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
package fm.common.rich

import scala.concurrent.{Future, ExecutionContext}
import scala.util.{Failure, Success}

object RichFuture {
  def apply[V](f: Future[V]): RichFuture[V] = if(f.isInstanceOf[RichFuture[_]]) f.asInstanceOf[RichFuture[V]] else new RichFuture(f)
  
  implicit def toRichFuture[V](f: Future[V]): RichFuture[V] = apply(f)
  
  /**
   * Similar to Future.find but instead of returning a Future[Option[T]] it just returns a Future[Boolean]
   */
  @inline def exists[T](futuretravonce: TraversableOnce[Future[T]])(predicate: T => Boolean)(implicit executor: ExecutionContext): Future[Boolean] = {
    // TODO: copy the Future.find implementation to avoid the extra wrapping of Future?
    val f: Future[Option[T]] = Future.find(futuretravonce)(predicate)
    f.map{ _.isDefined }
  }
}

// TODO: Figure out why I am forcing everything through the RichFuture.apply method...
final class RichFuture[V] private (val self: Future[V]) extends AnyVal {
  
  /**
   * Returns whether the future has been completed with a Success
   */
  def isSuccess: Boolean = self.value match {
    case Some(Success(_)) => true
    case _ => false
  }
  
  /**
   * Returns whether the future has been completed with a Failure
   */
  def isFailure: Boolean = self.value match {
    case Some(Failure(_)) => true
    case _ => false
  }
}