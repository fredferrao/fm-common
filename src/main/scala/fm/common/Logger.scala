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

object Logger {
  private[this] val hasSLF4J: Boolean = ClassUtil.classExists("org.slf4j.Logger")
  
  def getLogger(obj: AnyRef): Logger = if (hasSLF4J) SLF4JLogger(obj) else NoLogger
  
  object NoLogger extends Logger {
    def isTraceEnabled: Boolean = false
    def isDebugEnabled: Boolean = false
    def isInfoEnabled : Boolean = false
    def isWarnEnabled : Boolean = false
    def isErrorEnabled: Boolean = false
    
    def trace(msg: => String): Unit = {}
    def debug(msg: => String): Unit = {}
    def info(msg: => String) : Unit = {}
    def warn(msg: => String) : Unit = {}
    def error(msg: => String): Unit = {}
    
    def trace(msg: => String, ex: Throwable): Unit = {}
    def debug(msg: => String, ex: Throwable): Unit = {}
    def info(msg: => String, ex: Throwable) : Unit = {}
    def warn(msg: => String, ex: Throwable) : Unit = {}
    def error(msg: => String, ex: Throwable): Unit = {}
  }
  
  object SLF4JLogger {
    import org.slf4j.LoggerFactory
    
    def apply(obj: AnyRef): SLF4JLogger = new SLF4JLogger(getLoggerImpl(obj))
    
    private def getLoggerImpl(obj: AnyRef) = obj match {
      case s: String   => LoggerFactory.getLogger(s)
      case c: Class[_] => LoggerFactory.getLogger(loggerNameForClass(c.getName))
      case _           => LoggerFactory.getLogger(loggerNameForClass(obj.getClass.getName))
    }

    private def loggerNameForClass(className: String): String = {  
      if (className endsWith "$") className.substring(0, className.length - 1)  else className  
    }
  }
  
  final class SLF4JLogger(self: org.slf4j.Logger) extends Logger {
    def underlying: org.slf4j.Logger = self
    
    def isTraceEnabled: Boolean = self.isTraceEnabled()
    def isDebugEnabled: Boolean = self.isDebugEnabled()
    def isInfoEnabled : Boolean = self.isInfoEnabled()
    def isWarnEnabled : Boolean = self.isWarnEnabled()
    def isErrorEnabled: Boolean = self.isErrorEnabled()
    
    def trace(msg: => String): Unit = self.trace(msg)
    def debug(msg: => String): Unit = self.debug(msg)
    def info(msg: => String) : Unit = self.info(msg)
    def warn(msg: => String) : Unit = self.warn(msg)
    def error(msg: => String): Unit = self.error(msg)
    
    def trace(msg: => String, ex: Throwable): Unit = self.trace(msg, ex)
    def debug(msg: => String, ex: Throwable): Unit = self.debug(msg, ex)
    def info(msg: => String, ex: Throwable) : Unit = self.info(msg, ex)
    def warn(msg: => String, ex: Throwable) : Unit = self.warn(msg, ex)
    def error(msg: => String, ex: Throwable): Unit = self.error(msg, ex)
  }
}

trait Logger {
  def isTraceEnabled: Boolean
  def isDebugEnabled: Boolean
  def isInfoEnabled : Boolean
  def isWarnEnabled : Boolean
  def isErrorEnabled: Boolean
  
  def trace(msg: => String): Unit
  def debug(msg: => String): Unit
  def info(msg: => String) : Unit
  def warn(msg: => String) : Unit
  def error(msg: => String): Unit
  
  def trace(msg: => String, ex: Throwable): Unit
  def debug(msg: => String, ex: Throwable): Unit
  def info(msg: => String, ex: Throwable) : Unit
  def warn(msg: => String, ex: Throwable) : Unit
  def error(msg: => String, ex: Throwable): Unit
} 