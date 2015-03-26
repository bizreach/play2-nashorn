package jp.co.bizreach.play2nashorn

import java.io.{Reader, FileReader, File}
import javax.script.{ScriptContext, Bindings, SimpleScriptContext}

import play.api.Logger
import play.api.Play._
import play.api.mvc.{AnyContent, Request}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global


object Mustache extends Renderer {

  lazy val renderer = getRenderer("mustache")


  def apply(routeKey: String, jsonString: String, variables: Seq[(String, AnyRef)] = Seq())(implicit req: Request[_]): Future[String] =
    apply(routeKey, Some(jsonString), variables)

  def apply(routeKey: String)(implicit req: Request[_]): Future[String] =
    apply(routeKey, None, Seq())

  def sync(routeKey: String, jsonString: String, variables: Seq[(String, AnyRef)] = Seq())(implicit req: Request[_]): String =
    Await.result(apply(routeKey, Some(jsonString), variables), 60.seconds) // TODO make timeout configurable

  def sync(routeKey: String)(implicit req: Request[_]): String =
    Await.result(apply(routeKey, None, Seq()), 60.seconds) // TODO make timeout configurable


  private def apply(routeKey: String, jsonString: Option[String], variables: Seq[(String, AnyRef)])(implicit req: Request[_]): Future[String] = {

    val route = routeConf(routeKey)
    val (bind, ctx) = newBindingsAndContext
    val path = req.path

    Future {

      val res = s"var res = ${jsonString.getOrElse("{}")};"
      Logger.debug(res)
      engine.eval(res, ctx)

      variables.foreach { case (k, v) =>
        Logger.debug(s"Bind $k -> ${v.getClass.getCanonicalName}")
        bind.put(k ,v)
      }

      route.templates.foreach{ case (tplKey, tplPath) =>
        val resolvedPath = resolveTemplate(req, tplPath)
        if (nashorn.exists(resolvedPath)) {
          val script = s"var $tplKey = ${nashorn.readLines(resolvedPath)
            .map(_.replace("'", "\\'")).mkString("'", "' + \n'", "';")}"
          Logger.debug(s"Evaluating: $tplKey")
          engine.eval(script, ctx)
        } else
          Logger.warn(s"Template file: $tplKey($resolvedPath) was not found")
      }

      route.commons.foreach{ cmn =>
        getCommons(cmn).map { cmnUrl =>
          Logger.debug(s"Evaluating: $cmnUrl")
          engine.eval(readerOf(cmnUrl), ctx)
        }.getOrElse{
          Logger.warn(s"Common file: $cmn was not found")
        }
      }

      Logger.debug(s"Evaluating: $renderer")
      engine.eval(readerOf(renderer), ctx)

      route.scripts.foldLeft("") { case (acc, script) =>
        val scriptUrl = scriptPath(script)
        Logger.debug(s"Evaluating: $scriptUrl")
        engine.eval(readerOf(scriptUrl), ctx).asInstanceOf[String]
      }
    }
  }


}


object Dust extends Renderer {

  lazy val renderer = getRenderer("dust")

  // TODO implement Dust object


}


object React extends Renderer {

  lazy val renderer = getRenderer("mustache")

  // TODO implement React object
}


trait Renderer {
  lazy val engine = nashorn.engine
  lazy val commons = nashorn.commons


  protected[play2nashorn] lazy val nashorn = current.plugin[NashornPlugin].map(_.nashorn)
    .getOrElse(throw new IllegalStateException("NashornPlugin is not installed"))


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
