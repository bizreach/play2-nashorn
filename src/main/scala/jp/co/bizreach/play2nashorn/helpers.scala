package jp.co.bizreach.play2nashorn

import play.api.mvc.Request

trait TemplateResolver {


    def resolve(request:Request[_], originalPath:String): String

}


class DeviceAwareTemplateResolver extends TemplateResolver {

  val regex = """(.*)(-x)(\.html|\.htm)"""

  /**
   * xで終わる
   */
  override def resolve(request: Request[_], originalPath: String): String =
    originalPath.replaceFirst(regex, if (isMobile(request)) "$1-m$3" else "$1$3")


  /**
   * 超単純実装 (性能比較用)
   */
  def resolve0(request: Request[_], originalPath: String): String =
    if (originalPath.endsWith("-x.html"))
      if (isMobile(request))
        originalPath.substring(0, originalPath.length - 1 - 5) + "m.html"
      else
        originalPath.substring(0, originalPath.length - 2 - 5) + ".html"
    else if (originalPath.endsWith("-x.htm"))
      if (isMobile(request))
        originalPath.substring(0, originalPath.length - 1 - 4) + "m.htm"
      else
        originalPath.substring(0, originalPath.length - 2 - 4) + ".htm"
    else
      originalPath


  /**
   * 判断基準を以下のサイトに基づく
   * https://developer.mozilla.org/ja/docs/Browser_detection_using_the_user_agent
   */
  private def isMobile(request:Request[_]): Boolean =
    request
      .headers
      .getAll("User-Agent")
      .exists(ua => ua.contains("Mobi") && ! ua.contains("iPad"))
}