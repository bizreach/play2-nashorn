package jp.co.bizreach.play2nashorn

import java.io.{FileReader, File}
import javax.script.{Bindings, SimpleScriptContext}

import play.api.Logger
import play.api.Play._
import play.api.mvc.Request

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source


object Mustache extends Renderer {

  lazy val renderer = getRenderer("mustache")


  def apply(routeKey: String, jsonString: String)(implicit req: Request[_]): Future[String] =
    apply(routeKey, Some(jsonString))

  def apply(routeKey: String)(implicit req: Request[_]): Future[String] =
    apply(routeKey, None)

  def sync(routeKey: String, jsonString: String)(implicit req: Request[_]): String =
    Await.result(apply(routeKey, Some(jsonString)), 60.seconds) // TODO make timeout configurable

  def sync(routeKey: String)(implicit req: Request[_]): String =
    Await.result(apply(routeKey, None), 60.seconds) // TODO make timeout configurable


  private def apply(routeKey: String, jsonString: Option[String])(implicit req: Request[_]): Future[String] = {

    val route = routeConf(routeKey)
    val (bind, ctx) = newBindingsAndContext
    val path = req.path

    Future {

      val res = s"var res = ${jsonString.getOrElse("{}")};"
      Logger.debug(res)
      engine.eval(res, ctx)

      route.templates.foreach{ case (tplKey, tplPath) =>
        if (new File(basePath + tplPath).exists()) {
          val script = s"var $tplKey = ${Source.fromFile(basePath + tplPath)
            .getLines().map(_.replace("'", "\\'")).mkString("'", "' + \n'", "';")}"
          Logger.debug(s"Evaluating: $tplKey")
          engine.eval(script, ctx)
        } else
          Logger.warn(s"Template file: $tplKey($basePath$tplPath) was not found")
      }

      route.commons.foreach{ cmn =>
        getCommons(cmn).map { cmnUrl =>
          Logger.debug(s"Evaluating: ${cmnUrl.getPath}")
          engine.eval(new FileReader(cmnUrl), ctx)
        }.getOrElse{
          Logger.warn(s"Common file: $cmn was not found")
        }
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
  lazy val basePath = nashorn.basePath

  protected[play2nashorn] lazy val nashorn = current.plugin[NashornPlugin].map(_.nashorn)
    .getOrElse(throw new IllegalStateException("NashornPlugin is not installed"))


  protected[play2nashorn] def getRenderer(key: String):File  =
    nashorn.renderers.find(_._1 == key).get._2


  protected[play2nashorn] def getCommons(key: String):Option[File]  =
    nashorn.commons.find(_._1 == key).map(_._2)


  protected[play2nashorn] def newBindingsAndContext: (Bindings, SimpleScriptContext) =
    (nashorn.engine.createBindings(), new SimpleScriptContext)


  protected[play2nashorn] def scriptPath(requestPath: String):File = {
    val path = if(requestPath.endsWith(".js")) requestPath.substring(0, requestPath.length - 3) else requestPath
    new File(s"${nashorn.basePath}/$path.js")
  }


  protected[play2nashorn] def routeConf(key: String): RouteConfig =
    nashorn.routes(key)
}
