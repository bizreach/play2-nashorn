package jp.co.bizreach.play2nashorn


import java.io.{FileReader, File}
import javax.script.{ScriptEngineManager, ScriptContext, SimpleScriptContext, ScriptEngine}

import com.typesafe.config.ConfigRenderOptions
import play.api.{Configuration, Logger, Plugin, Application}

case class RouteConfig(
  key:String,
  commons:Seq[String],
  templates:Seq[(String, String)],
  scripts:Seq[String],
  configParams: Option[String])



class NashornPlugin(app: Application) extends Plugin {

  private val root = "play2nashorn"


  trait Nashorn {
    val basePath: String
    val engine: ScriptEngine
    val renderers: Seq[(String, File)]
    val commons: Seq[(String, File)]
    val routes: Map[String, RouteConfig]

    def touch(): Unit = {
      val b = engine.createBindings()
      val context = new SimpleScriptContext()
      context.setBindings(b, ScriptContext.ENGINE_SCOPE)
      renderers.map { case (key, reader) =>
        Logger.debug(s"Initialize renderer: $key -> ${reader.toString}")
        engine.eval(new FileReader(reader), context)
      }
      commons.map { case (key, reader) =>
        Logger.debug(s"Initialize common file: $key -> ${reader.toString}")
        engine.eval(new FileReader(reader), context)
      }
    }
  }


  lazy val nashorn = new Nashorn {
    val basePath = configString(s"$root.basePath", "/public")

    // Pass 'null' to force the correct class loader. Without passing any param,
    // the "nashorn" JavaScript engine is not found by the `ScriptEngineManager`.
    //
    // See: https://github.com/playframework/playframework/issues/2532
    val engine = new ScriptEngineManager(null).getEngineByName("nashorn")
//    val engine = new NashornScriptEngineFactory().getScriptEngine


    val renderers = configStringSeq(app.configuration, s"$root.renderers").map{case (k, v) =>
      k -> new File(basePath + v)}
    val commons = configStringSeq(app.configuration, s"$root.commons").map{case (k, v) =>
      k -> new File(basePath + v)}
    val routes = initRouteConfig(s"$root.routes")
  }


  override def onStart(): Unit = {
    super.onStart()
    Logger.info("Initialize Nashorn plugin ...")
    try {
      nashorn.touch()

    } catch {
      case ex:NoClassDefFoundError =>
        if (ex.getMessage.contains("NashornScriptEngineFactory")) {
          Logger.error("NashornScriptEngineFactory was not found.")
          Logger.error("The server needs JDK 8, but it's working on:")
          Logger.error(System.getProperties.get("java.runtime.name").toString)
          Logger.error(System.getProperties.get("java.runtime.version").toString)
        }
        throw ex
    }
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
        templates = configStringSeq(c, s"$key.templates"),
        scripts = c.getStringSeq(s"$key.scripts").getOrElse(Seq()),
        configParams = c.getConfig(s"$key.configParams").map(_.underlying.root().render(ConfigRenderOptions.concise()))
      )
    }.toMap}.getOrElse(Map())
  }


  protected def configStringSeq(root: Configuration, key: String):Seq[(String, String)] = {
    root.getConfigSeq(key) match {
      case Some(list) =>
        list.flatMap { conf =>
          conf.subKeys.headOption.map{ subKey =>
            subKey -> conf.getString(subKey).getOrElse("not-string")
          }
        }

      case None =>
        Seq()
    }

  }

  protected def configStringMap(key: String): Map[String, String] =
    app.configuration.getConfig(key).map {
      conf => conf.subKeys.map { fileKey =>
        fileKey -> conf.getString(fileKey).getOrElse("not-found")
      }.toMap
    }.getOrElse(Map())


  protected def configString(key: String, default: String): String =
    app.configuration.getString(key).getOrElse(default)
}


