package jp.co.bizreach.play2nashorn.sandbox

import java.io.File
import java.net.URL
import javax.script._

import jdk.nashorn.api.scripting.{NashornScriptEngineFactory, URLReader}
import org.scalatest.{FunSpec, Matchers}

class NashornCacheSampleSpec extends FunSpec with Matchers {

  describe("Nashorn Sample") {
    describe("ClassCacheDemo") {
      it("should run js compilation with cache") {
        println("*** ClassCacheDemo ***")
//        ClassCacheDemo.main(Array())
      }
    }

    describe("ThreadedClassCacheDemo") {
      it("should run js compilation with cache") {
        println("*** ThreadedClassCacheDemo ***")
//        ThreadedClassCacheDemo.main(Array())
      }
    }

    describe("PersistentClassCacheDemo") {
      it("should run js compilation with cache") {
        println("*** PersistentClassCacheDemo ***")
//        PersistentClassCacheDemo.main(Array())
      }
    }
  }

}


/**
 * https://blogs.oracle.com/nashorn/entry/improving_nashorn_startup_time_using
 * http://hns.github.io/files/codecache-demos/ClassCacheDemo.java
 */
object ClassCacheDemo {

  def main(args:Array[String]):Unit = {
    // Get a Nashorn script engine with default options
    val engine = new NashornScriptEngineFactory().getScriptEngine()
//    val engine = new ScriptEngineManager(null).getEngineByName("nashorn")
    val react = new File("src/test/resources/react-with-addons.min.js").toURI().toURL()

    // Evaluate the script 20 times, using the same script engine but a new global bindings each time.
    // This allows the compiled script to be reused with JDK 8u20 and later.
    (0 to 20).foreach { i =>
      evaluateInNewScope(engine, react)
    }
  }

  def evaluateInNewScope(engine: ScriptEngine, url: URL): Unit = {
    val b = engine.createBindings()
    val context = new SimpleScriptContext()
    context.setBindings(b, ScriptContext.ENGINE_SCOPE)

    val start = System.currentTimeMillis()
    try {
      engine.eval(new URLReader(url), context)
//      engine.eval(new FileReader("src/test/resources/react.js"))
    } finally {
      System.out.println("Evaluated " + url + " in " + (System.currentTimeMillis() - start) + " milliseconds")
    }
  }
}


/**
 * https://blogs.oracle.com/nashorn/entry/improving_nashorn_startup_time_using
 * http://hns.github.io/files/codecache-demos/ThreadedClassCacheDemo.java
 */
object ThreadedClassCacheDemo {

  def main(args: Array[String]):Unit = {
    // Get a Nashorn script engine with default options
    val engine = new NashornScriptEngineFactory().getScriptEngine()
    val react = new File("src/test/resources/react-with-addons.min.js").toURI().toURL()

    // Evaluate the script 20 times, using the same script engine but a new thread and global bindings
    // for each iteration. This allows the compiled script to be reused with JDK 8u20 and later.
    (0 to 20).foreach { i =>
      runInThread(engine, react)
    }
  }

  def runInThread(engine: ScriptEngine, url: URL):Unit = {
    new Thread() {
      override def run():Unit = {
        val b = engine.createBindings()
        val context = new SimpleScriptContext()
        context.setBindings(b, ScriptContext.ENGINE_SCOPE)

        val start = System.currentTimeMillis()
        try {
          engine.eval(new URLReader(url), context);
        } catch {
          case e:ScriptException =>
            System.err.println("Error evaluating script: " + e)
        }
        System.out.println("Evaluated " + url + " in " + (System.currentTimeMillis() - start) + " milliseconds")
      }
    }.run()
  }
}


object PersistentClassCacheDemo {

  def main(args: Array[String]):Unit = {
    val react = new File("src/test/resources/react-with-addons.min.js").toURI().toURL()

    // Evaluate the script 20 times using the persistent code cache.
    // We create a new script engine on each iteration to disable internal class caching.
    (0 to 20).foreach { i =>
      // Get a Nashorn script engine with persistent code caching enabled.
      // Note that this option will only work with JDK 8u20 or later.
      val engine = new NashornScriptEngineFactory().getScriptEngine(Array("-pcc"), this.getClass.getClassLoader)
      evaluate(engine, react)
    }
  }

  def evaluate(engine: ScriptEngine, url: URL): Unit = {
    val start = System.currentTimeMillis()
    try {
      engine.eval(new URLReader(url))
    } finally {
      System.out.println("Evaluated " + url + " in " + (System.currentTimeMillis() - start) + " milliseconds")
    }
  }
}