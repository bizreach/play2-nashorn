package jp.co.bizreach.play2nashorn

import java.io.{FileReader, File}
import javax.script.{Bindings, SimpleScriptContext}

import play.api.Logger
import play.api.Play._
import play.api.mvc.Request

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global



object Mustache extends Renderer {

  lazy val renderer = nashorn.renderers("mustache")
  lazy val engine = nashorn.engine
  lazy val commons = nashorn.commons


  // 1. Get configuration from the config based on the request path
  // 2. Read additional file paths
  //     - partial template especially.
  // 3. Read additional JSON value by converting config to JSON string
  def apply(routeKey: String, json: String)(implicit req: Request[_]): Future[String] = {

    val route = routeConf(routeKey)
    val (bind, ctx) = newBindingsAndContext
    val path = req.path
    val res = """var res = {};"""

    Future {
      engine.eval(res, ctx)

      // TODO eval templates

      route.commons.foreach{cmn =>
        val cmnUrl = commons(cmn)
        Logger.debug(s"Evaluating: ${cmnUrl.getPath}")
        engine.eval(new FileReader(cmnUrl), ctx)
      }

      Logger.debug(s"Evaluating: ${renderer.getPath}")
      engine.eval(new FileReader(renderer), ctx)

      route.scripts.foldLeft("") { case (acc, script) =>
        val scriptUrl = scriptPath(script)
        Logger.debug(s"Evaluating: ${scriptUrl.getPath}")
        engine.eval(new FileReader(scriptUrl), ctx).asInstanceOf[String]
      }
    }
  }


  def sync(routeKey: String, json: String)(implicit req: Request[_]): String = {
    Await.result(apply(routeKey, json), 60.seconds) // TODO make timeout configurable
  }
  

  def apply(path: String, json: Any): Future[String] = {
    renderer //...

    ???
  }
}


object Dust extends Renderer {

  lazy val renderer = nashorn.renderers("dust")

  // TODO implement Dust object


}


object React extends Renderer {

  lazy val renderer = nashorn.renderers("mustache")

  // TODO implement React object
}


trait Renderer {

  protected[play2nashorn] lazy val nashorn = current.plugin[NashornPlugin].map(_.nashorn)
    .getOrElse(throw new IllegalStateException("NashornPlugin is not installed"))

  protected[play2nashorn] def newBindingsAndContext: (Bindings, SimpleScriptContext) =
    (nashorn.engine.createBindings(), new SimpleScriptContext)

  protected[play2nashorn] def scriptPath(requestPath: String):File = {
    val path = if(requestPath.endsWith(".js")) requestPath.substring(0, requestPath.length - 3) else requestPath
    new File(s"${nashorn.basePath}/$path.js")
  }

  protected[play2nashorn] def routeConf(key: String): RouteConfig =
    nashorn.routes(key)
}
