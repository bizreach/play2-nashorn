package jp.co.bizreach.play2nashorn

import java.io.File
import javax.script.{ScriptContext, SimpleScriptContext}

import jdk.nashorn.api.scripting.{URLReader, NashornScriptEngineFactory}
import org.scalatest.{Matchers, FunSpec}

/**
 * Created by satoshi.kobayashi on 3/8/15.
 */
class ReactTutorialSpec extends FunSpec with Matchers {

  describe("React tutorial") {
    it("should run properly") {
      val engine = new NashornScriptEngineFactory().getScriptEngine()
      val react = new File("src/test/resources/react-with-addons.js").toURI().toURL()
      val jsx = new File("src/test/resources/JSXTransformer.js").toURI().toURL()
      val example = new File("src/test/resources/tutorial/example.js").toURI().toURL()

      val b = engine.createBindings()
      val context = new SimpleScriptContext()
      context.setBindings(b, ScriptContext.ENGINE_SCOPE)

      val start = System.currentTimeMillis()

      engine.eval(new URLReader(react), context)
      engine.eval(new URLReader(jsx), context)
      val result = engine.eval(new URLReader(example), context)

      println(result)
      println(s"Evaluated in ${System.currentTimeMillis() - start} milliseconds")
    }
  }
}
