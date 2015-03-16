package controllers

import jp.co.bizreach.play2nashorn.Mustache
import play.api.mvc._
import play.api.libs.json.Json
import play.twirl.api.{HtmlFormat, Html}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


object Application extends Controller {

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }


  /**
   * Simple rendering with Mustache
   */
  def mustache1 = Action.async { implicit req =>

//    Mustache("mustache-1").map{html => Ok(new Html(html))}
    Mustache("mustache-1").map{html => Ok(HtmlFormat.raw(html))}
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

    val result: Future[String] = Mustache("mustache-2", json.toString())

    result.map{ html => Ok(HtmlFormat.raw(html)) }
  }

}