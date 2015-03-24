package jp.co.bizreach.play2nashorn


import java.io._
import javax.script.{ScriptEngineManager, ScriptContext, SimpleScriptContext, ScriptEngine}

import com.typesafe.config.ConfigRenderOptions
import play.api._

import scala.io.Source

case class RouteConfig(
  key:String,
  commons:Seq[String],
  templates:Seq[(String, String)],
  scripts:Seq[String],
  configParams: Option[String])



class NashornPlugin(app: Application) extends Plugin {

  private val root = "play2nashorn"


  lazy val nashorn = new NashornConfig {
    // Pass 'null' to force the correct class loader. Without passing any param,
    // the "nashorn" JavaScript engine is not found by the `ScriptEngineManager`.
    //
    // See: https://github.com/playframework/playframework/issues/2532
    val engine = new ScriptEngineManager(null).getEngineByName("nashorn")
    //    val engine = new NashornScriptEngineFactory().getScriptEngine

    val basePath = configString(s"$root.basePath", "/public")
    val readClassPath = configBoolean(s"$root.readClassPath", Play.isProd(Play.current))

    val renderers = configStringSeq(app.configuration, s"$root.renderers")
    val commons = configStringSeq(app.configuration, s"$root.commons")
    val routes = initRouteConfig(s"$root.routes")
    val templateResolvers = configStringSeq(s"$root.templateResolvers")
      .map(_.map(loadClass[TemplateResolver])).getOrElse(Seq(new DeviceAwareTemplateResolver))
  }


  override def onStart(): Unit = {
    super.onStart()
    Logger.info("Initialize Nashorn plugin ...")
    try {
      nashorn.touch()

      Logger.debug(s"play2-nashorn: basePath: ${nashorn.basePath}")
      Logger.debug(s"play2-nashorn: readClassPath: ${nashorn.readClassPath}")
      Logger.debug(s"play2-nashorn: renderers: ${nashorn.renderers}")
      Logger.debug(s"play2-nashorn: commons: ${nashorn.commons}")
      Logger.debug(s"play2-nashorn: routes: ${nashorn.routes}")

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
    Logger.info("play2-nashorn is shutting down, Bye !")
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


  protected def configBoolean(key: String, default: Boolean): Boolean =
    app.configuration.getBoolean(key).getOrElse(default)


  protected def configStringSeq(key: String): Option[Seq[String]] =
    app.configuration.getStringSeq(key)


  private def loadClass[T](clazz: String): T = {
    Logger.debug(s"Loading $clazz")
    app.classloader.loadClass(clazz).newInstance().asInstanceOf[T]
  }
}


trait NashornConfig {
  val engine: ScriptEngine
  val basePath: String
  val readClassPath: Boolean
  val renderers: Seq[(String, String)]
  val commons: Seq[(String, String)]
  val routes: Map[String, RouteConfig]
  val templateResolvers: Seq[TemplateResolver]

  def touch(): Unit = {
    val b = engine.createBindings()
    val context = new SimpleScriptContext()
    context.setBindings(b, ScriptContext.ENGINE_SCOPE)
    renderers.map { case (key, reader) =>
      Logger.debug(s"Initialize renderer: $key -> ${reader.toString}")
      engine.eval(readerOf(reader), context)
    }
    commons.map { case (key, reader) =>
      Logger.debug(s"Initialize common file: $key -> ${reader.toString}")
      engine.eval(readerOf(reader), context)
    }
  }


  def readerOf(path: String): Reader =
    if (readClassPath) {
      if (this.getClass.getClassLoader.getResourceAsStream(s"$basePath$path") != null)
        new BufferedReader(new InputStreamReader(this.getClass.getClassLoader.getResourceAsStream(s"$basePath$path")))
      else
        throw new RuntimeException(s"File: $basePath$path was not found in the classpath")
    }
    else
      new FileReader(new File(s"$basePath$path"))


  def exists(path: String): Boolean =
    if (readClassPath)
      this.getClass.getClassLoader.getResourceAsStream(s"$basePath$path") != null
    else
      new File(s"$basePath$path").exists()


  def readLines(path: String): Iterator[String] =
    if (readClassPath)
      Source.fromInputStream(this.getClass.getClassLoader.getResourceAsStream(s"$basePath$path")).getLines()
    else
      Source.fromFile(s"$basePath$path").getLines()
}
