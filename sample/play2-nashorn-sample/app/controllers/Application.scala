package controllers

import jp.co.bizreach.play2nashorn.Mustache
import play.api.mvc._
import play.api.libs.json.Json
import play.twirl.api.{HtmlFormat, Html}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class Application extends Controller {

  def index = Action {
    Ok(views.html.index("Your new application is ready with play2-nashorn."))
  }


  /**
   * Simple rendering with Mustache
   */
  def mustache1 = Action.async { implicit req =>

    Mustache("mustache-1").map{html => Ok(HtmlFormat.raw(html.toString()))}
  }


  /**
   * Mustache rendering including
   *  - JSON result passing
   *  - partial templates
   */
  def mustache2 = Action.async { implicit req =>

    val json = Json.obj(
      "root" -> "house",
      "parts" -> Json.obj("part1" -> "door", "part2" -> "window"))

    val result: Future[String] = Mustache("mustache-2", json.toString()).map(_.toString())

    result.map{ html => Ok(HtmlFormat.raw(html)) }
  }


  /**
   * Prepare 2 templates for Desktop and mobile
   */
  def mustache3 = Action.async { implicit req =>
    Mustache("mustache-3").map{ html => Ok(HtmlFormat.raw(html.toString())) }
  }
}