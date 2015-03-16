package jp.co.bizreach.play2nashorn

import java.io.File
import javax.script.{ScriptContext, SimpleScriptContext}

import com.fasterxml.jackson.databind.ObjectMapper
import jdk.nashorn.api.scripting.{NashornScriptEngineFactory, URLReader}
import org.scalatest.{FunSpec, Matchers}

import scala.concurrent.Promise
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by satoshi.kobayashi on 3/8/15.
 */
class DustSampleSpec extends FunSpec with Matchers {

  describe("Dust") {
    it("should run properly") {
      val engine = new NashornScriptEngineFactory().getScriptEngine
      val dust = new File("src/test/resources/dust-full.min.js").toURI.toURL
      val json = """{"corporate":"BizReach", "jobs":["engineer", "sales", "designer"], "isApplicable":true}"""
      val mapper = new ObjectMapper()
      val compiledJson = mapper.readTree(json)

//      val partial2 = "var partial2 = " + Source.fromFile("src/test/resources/mustache/mustache-test1.html").getLines().mkString("'", "' + \n'", "';")
      val template =  new File("src/test/resources/dust/dust-test1.js").toURI.toURL

      val b = engine.createBindings()
      val context = new SimpleScriptContext()
      context.setBindings(b, ScriptContext.ENGINE_SCOPE)
//      val scope = engine.getBindings(ScriptContext.ENGINE_SCOPE)

      val start = System.currentTimeMillis()

//      scope.put("window", scope)
      engine.eval(new URLReader(dust), context)
//      engine.eval(json, context)
//      engine.eval(partial2, context)
      val (promise, writer) = TemplateWriter()

      engine.eval("var res =" + json, context)
      b.put("res2",compiledJson)
      b.put("writer", writer)
      val result = engine.eval(new URLReader(template), context)

      println(result)
      println(s"Evaluated in ${System.currentTimeMillis() - start} milliseconds")

      promise.future.onSuccess {
        case rendered:String => println (s"Successfully: $rendered")
      }
    }
  }
}

object ResponseReceiver {
  var callback: JsCallback = _
  var info:String = _

  def echo(out:String):String = {
    System.out.println("JAVA echo")
    out
  }


  def setCallback(callback: JsCallback):Unit = {
    System.out.println("JAVA setCallback")
    this.callback = callback
  }
}

class TemplateWriter(promise: Promise[String]) {
  def flush(template: String): Unit = {
    println(s"Flashing: template")
    promise.success(template)
    println("Flashed")
  }
}

object TemplateWriter {

  def apply():(Promise[String], TemplateWriter) = {
    val promise = Promise[String]()
    (promise, new TemplateWriter(promise))
  }

}

@FunctionalInterface
trait JsCallback {
  def onEvent(name:String, rendered:String):Unit
}