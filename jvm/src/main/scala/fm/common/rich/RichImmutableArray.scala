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
package fm.common.rich

import fm.common.{ImmutableArray, Interner, LoadingCache}

object RichImmutableArray {
  private val interners: LoadingCache[Class[_], Interner[ImmutableArray[_]]] = LoadingCache(){ cls: Class[_] => Interner() }
}

final class RichImmutableArray[A](val arr: ImmutableArray[A]) extends AnyVal {
  def intern: ImmutableArray[A] = RichImmutableArray.interners.get(arr.getClass)(arr).asInstanceOf[ImmutableArray[A]]
}