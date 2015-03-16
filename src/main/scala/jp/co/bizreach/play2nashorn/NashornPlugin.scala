package jp.co.bizreach.play2nashorn


import java.io.File
import javax.script.{ScriptContext, SimpleScriptContext, ScriptEngine}

import com.typesafe.config.ConfigRenderOptions
import jdk.nashorn.api.scripting.{URLReader, NashornScriptEngineFactory}
import play.api.{Logger, Plugin, Application}

case class RouteConfig(
  key:String,
  commons:Seq[String],
  templates:Seq[String],
  scripts:Seq[String],
  configParams: Option[String])



class NashornPlugin(app: Application) extends Plugin {

  private val root = "play2nashorn"


  trait Nashorn {
    val basePath: String
    val engine: ScriptEngine
    val renderers: Map[String, URLReader]
    val commons: Map[String, URLReader]
    val routes: Map[String, RouteConfig]

    def touch(): Unit = {
      val b = engine.createBindings()
      val context = new SimpleScriptContext()
      context.setBindings(b, ScriptContext.ENGINE_SCOPE)
      renderers.map { case (key, urlReader) =>
        Logger.debug(s"Initialize renderer: $key -> ${urlReader.getURL}")
        engine.eval(urlReader, context)
      }
      commons.map { case (key, urlReader) =>
        Logger.debug(s"Initialize common file: $key -> ${urlReader.getURL}")
        engine.eval(urlReader, context)
      }
    }
  }


  lazy val nashorn = new Nashorn {
    val basePath = configString(s"$root.basePath", "/public")
    val engine = new NashornScriptEngineFactory().getScriptEngine
    val renderers = configStringMap(s"$root.renderers").map{case (k, v) =>
      k -> new URLReader(new File(basePath + v).toURI.toURL)}
    val commons = configStringMap(s"$root.commons").map{case (k, v) =>
      k -> new URLReader(new File(basePath + v).toURI.toURL)}
    val routes = initRouteConfig(s"$root.routes")
  }


  override def onStart(): Unit = {
    super.onStart()
    Logger.info("Initialize Nashorn plugin ...")
    nashorn.touch()
    Logger.info("Nashorn initialization has completed.")
  }


  override def onStop(): Unit = {
    Logger.info("Bye !")
    super.onStop()
  }


  protected def initRouteConfig(path: String):Map[String, RouteConfig] = {
    app.configuration.getConfig(path).map{c => c.subKeys.map { key =>
      key -> RouteConfig(
        key = key,
        commons = c.getStringSeq(s"$key.commons").getOrElse(Seq()),
        templates = c.getStringSeq(s"$key.templates").getOrElse(Seq()),
        scripts = c.getStringSeq(s"$key.scripts").getOrElse(Seq()),
        configParams = c.getConfig(s"$key.configParams").map(_.underlying.root().render(ConfigRenderOptions.concise()))
      )
    }.toMap}.getOrElse(Map())
  }


  protected def configStringMap(key: String):Map[String, String] = {
    app.configuration.getConfig(key).map {
      conf => conf.subKeys.map { fileKey =>
        fileKey -> conf.getString(fileKey).getOrElse("not-found")
      }.toMap
    }.getOrElse(Map())
  }


  protected def configString(key: String, default: String): String =
    app.configuration.getString(key).getOrElse(default)
}


