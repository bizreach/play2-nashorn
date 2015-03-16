package controllers

import jp.co.bizreach.play2nashorn.Mustache
import play.api.mvc._
import play.twirl.api.{HtmlFormat, Html}
import scala.concurrent.ExecutionContext.Implicits.global


object Application extends Controller {

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def mustache1 = Action.async { implicit req =>

//    Mustache("mustache-1", "{}").map{html => Ok(new Html(html))}
    Mustache("mustache-1", "{}").map{html => Ok(HtmlFormat.raw(html))}
  }

}