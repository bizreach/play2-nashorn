package jp.co.bizreach.play2nashorn

import play.api.mvc.Request

trait TemplateResolver {


    def resolve(request:Request[_], originalPath:String): String

}

/**
 *
 */
class DeviceAwareTemplateResolver extends TemplateResolver {

  val regex = """(.*)(-x)(\.html|\.htm)"""

  /**
   * If the template name part (excluding file extension) ends with 'x',*
   * return -x in mobile, no post-fix in desktop
   */
  override def resolve(request: Request[_], originalPath: String): String =
    originalPath.replaceFirst(regex, if (isMobile(request)) "$1-m$3" else "$1$3")


  /**
   * See the url below
   * https://developer.mozilla.org/ja/docs/Browser_detection_using_the_user_agent
   */
  private def isMobile(request:Request[_]): Boolean =
    request
      .headers
      .getAll("User-Agent")
      .exists(ua => ua.contains("Mobi") && !ua.contains("iPad"))
}