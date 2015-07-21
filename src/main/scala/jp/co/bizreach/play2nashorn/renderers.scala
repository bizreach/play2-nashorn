package jp.co.bizreach.play2nashorn

import java.io.Reader
import javax.script.{ScriptEngine, ScriptContext, Bindings, SimpleScriptContext}

import play.api.Logger
import play.api.Play._
import play.api.mvc.Request
import play.twirl.api.{Html, HtmlFormat}

import scala.concurrent.{Promise, Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global


/**
 * Mustache
 * - sync renderer
 */
object Mustache extends SyncRenderer {

  lazy val renderer = getRenderer("mustache")

}


/**
 * React
 * - sync renderer
 */
object React extends SyncRenderer {

  lazy val renderer = getRenderer("react")

}


/**
 * Dust
 * - sync renderer
 */
object Dust extends AsyncRenderer {

  lazy val renderer = getRenderer("dust")

}


trait AsyncRenderer extends Renderer {

  protected[play2nashorn] def apply(routeKey: String, jsonString: Option[String], variables: Seq[(String, AnyRef)])
                    (implicit req: Request[_]): Future[Html] = {

    val route = routeConf(routeKey)
    val (bind, ctx) = newBindingsAndContext
    val path = req.path

    evaluateResponse(ctx, jsonString)

    val (promise, writer) = TemplateWriter()
    bind.put("writer", writer)

    bindVariables(bind, variables)

    evaluateTemplates(ctx, route)

    evaluateCommons(ctx, route)

    evaluateRenderer(ctx)

    render(ctx, route)

    promise.future
  }


  protected[play2nashorn] def render(ctx: ScriptContext, route: RouteConfig): Unit = {
    route.scripts.foreach { script =>
      val scriptUrl = scriptPath(script)
      Logger.debug(s"Evaluating: $scriptUrl")
      engine.eval(readerOf(scriptUrl), ctx)
    }

  }
}


trait SyncRenderer extends Renderer {

  protected[play2nashorn] def apply(routeKey: String, jsonString: Option[String], variables: Seq[(String, AnyRef)])
                                   (implicit req: Request[_]): Future[Html] = {

    val route = routeConf(routeKey)
    val (bind, ctx) = newBindingsAndContext
    val path = req.path

    Future {

      evaluateResponse(ctx, jsonString)

      bindVariables(bind, variables)

      evaluateTemplates(ctx, route)

      evaluateCommons(ctx, route)

      evaluateRenderer(ctx)

      val htmlText = render(ctx, route)

      HtmlFormat.raw(htmlText)
    }
  }


  protected[play2nashorn] def render(ctx: ScriptContext, route: RouteConfig): String = {
    route.scripts.foldLeft("") { case (acc, script) =>
      val scriptUrl = scriptPath(script)
      Logger.debug(s"Evaluating: $scriptUrl")
      engine.eval(readerOf(scriptUrl), ctx).asInstanceOf[String]
    }
  }

}


trait Renderer {
  private[this] var _config: Option[NashornConfig] = None

  lazy val engine = nashorn.engine
  lazy val commons = nashorn.commons


  protected[play2nashorn] def injectConfig(config: NashornConfig): Unit = {
    this._config.foreach{ _ => Logger.warn("")}
    this._config = Some(config)
  }

  protected[play2nashorn] def nashorn: NashornConfig = {
    _config.getOrElse(throw new IllegalStateException("HandlebarsPlugin is not installed"))
  }


  def renderer: String


  def apply(routeKey: String, jsonString: String, variables: Seq[(String, AnyRef)] = Seq())(implicit req: Request[_]): Future[Html] =
    apply(routeKey, Some(jsonString), variables)


  def apply(routeKey: String)(implicit req: Request[_]): Future[Html] =
    apply(routeKey, None, Seq())


  def sync(routeKey: String, jsonString: String, variables: Seq[(String, AnyRef)] = Seq())(implicit req: Request[_]): Html =
    Await.result(apply(routeKey, Some(jsonString), variables), Duration.Inf)


  def sync(routeKey: String)(implicit req: Request[_]): Html =
    Await.result(apply(routeKey, None, Seq()), Duration.Inf)


  protected[play2nashorn] def apply(routeKey: String, jsonString: Option[String], variables: Seq[(String, AnyRef)])
                                   (implicit req: Request[_]): Future[Html]


  protected[play2nashorn] def evaluateResponse(ctx: ScriptContext, jsonString: Option[String]): Unit = {
    val res = s"var res = ${jsonString.getOrElse("{}")};"
    Logger.debug(res)
    engine.eval(res, ctx)
  }

  protected[play2nashorn] def bindVariables(bind: Bindings, variables: Seq[(String, AnyRef)]): Unit = {
    variables.foreach { case (k, v) =>
      Logger.debug(s"Bind $k -> ${v.getClass.getCanonicalName}")
      bind.put(k, v)
    }
  }


  protected[play2nashorn] def evaluateTemplates(ctx: ScriptContext, route: RouteConfig)(implicit req: Request[_]): Unit = {
    route.templates.foreach { case (tplKey, tplPath) =>
      val resolvedPath = resolveTemplate(req, tplPath)
      if (nashorn.exists(resolvedPath)) {
        val script = s"var $tplKey = ${
          nashorn.readLines(resolvedPath)
            .map(_.replace("'", "\\'")).mkString("'", "' + \n'", "';")
        }"
        Logger.debug(s"Evaluating: $tplKey")
        engine.eval(script, ctx)
      } else
        Logger.warn(s"Template file: $tplKey($resolvedPath) was not found")
    }
  }


  protected[play2nashorn] def evaluateCommons(ctx: ScriptContext, route: RouteConfig): Unit = {
    route.commons.foreach { cmn =>
      getCommons(cmn).map { cmnUrl =>
        Logger.debug(s"Evaluating: $cmnUrl")
        engine.eval(readerOf(cmnUrl), ctx)
      }.getOrElse {
        Logger.warn(s"Common file: $cmn was not found")
      }
    }
  }


  protected[play2nashorn] def evaluateRenderer(ctx: ScriptContext): Unit = {
    Logger.debug(s"Evaluating: $renderer")
    engine.eval(readerOf(renderer), ctx)

  }


  protected[play2nashorn] def getRenderer(key: String):String  =
    nashorn.renderers.find(_._1 == key).get._2


  protected[play2nashorn] def getCommons(key: String):Option[String]  =
    nashorn.commons.find(_._1 == key).map(_._2)


  protected[play2nashorn] def newBindingsAndContext: (Bindings, SimpleScriptContext) = {
    val binding = nashorn.engine.createBindings()
    val context = new SimpleScriptContext
    context.setBindings(binding, ScriptContext.GLOBAL_SCOPE)
    (binding, context)
  }


  protected[play2nashorn] def scriptPath(requestPath: String):String = {
    val path = if(requestPath.endsWith(".js")) requestPath.substring(0, requestPath.length - 3) else requestPath
    s"$path.js"
  }


  protected[play2nashorn] def routeConf(key: String): RouteConfig =
    nashorn.routes(key)


  protected[play2nashorn] def readerOf(path: String): Reader =
    nashorn.readerOf(path)


  protected[play2nashorn] def resolveTemplate(req: Request[_], path: String): String =
    nashorn.templateResolvers.foldLeft(path){ case (out, resolver) =>
      resolver.resolve(req, out)
    }
}


class TemplateWriter(promise: Promise[Html]) {
  def flush(template: String): Unit = {
    Logger.debug(s"Flashing: template")
    promise.success(HtmlFormat.raw(template))
    Logger.debug("Flashed")
  }
}


object TemplateWriter {
  def apply():(Promise[Html], TemplateWriter) = {
    val promise = Promise[Html]()
    (promise, new TemplateWriter(promise))
  }
}