package jp.co.bizreach.play2nashorn

import java.io.File
import javax.script.{ScriptContext, SimpleScriptContext}

import jdk.nashorn.api.scripting.{NashornScriptEngineFactory, URLReader}
import org.scalatest.{FunSpec, Matchers}

import scala.io.Source

/**
 * Created by satoshi.kobayashi on 3/8/15.
 */
class MustacheTestSpec extends FunSpec with Matchers {

  describe("Mustache") {
    it("should run properly") {
      val engine = new NashornScriptEngineFactory().getScriptEngine
      val mustache = new File("src/test/resources/mustache.min.js").toURI.toURL
      val json = """var res = {"corporate":"BizReach", "jobs":["engineer", "sales", "designer"], "isApplicable":true}"""
      val partial2 = "var partial2 = " + Source.fromFile("src/test/resources/mustache/mustache-test1.html").getLines().mkString("'", "' + \n'", "';")
      val template =  new File("src/test/resources/mustache/mustache-test1.js").toURI.toURL

      val b = engine.createBindings()
      val context = new SimpleScriptContext()
      context.setBindings(b, ScriptContext.ENGINE_SCOPE)
//      val scope = engine.getBindings(ScriptContext.ENGINE_SCOPE)

      val start = System.currentTimeMillis()

//      scope.put("window", scope)
      engine.eval(new URLReader(mustache), context)
      engine.eval(json, context)
      engine.eval(partial2, context)
      val result = engine.eval(new URLReader(template), context)

      println(result)
      println(s"Evaluated in ${System.currentTimeMillis() - start} milliseconds")
    }
  }
}
