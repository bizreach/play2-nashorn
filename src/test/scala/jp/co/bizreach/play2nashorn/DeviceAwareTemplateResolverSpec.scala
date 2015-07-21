package jp.co.bizreach.play2nashorn

import org.scalatest.{Matchers, FunSpec}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.{FakeHeaders, FakeRequest}

/**
 * Created by satoshi.kobayashi on 3/24/15.
 */
class DeviceAwareTemplateResolverSpec extends FunSpec with Matchers {

  val aDesktopUserAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2272.89 Safari/537.36"
  val aMobileUserAgent = "Mozilla/5.0 (iPhone; CPU iPhone OS 8_0 like Mac OS X) AppleWebKit/600.1.3 (KHTML, like Gecko) Version/8.0 Mobile/12A4345d Safari/600.1.4"

  val resolver = new DeviceAwareTemplateResolver

  describe("DeviceAwareTemplateResolver") {
    it("should resolve template when it ends with -x in a request from desktop ") {
      val desktopRequest = FakeRequest("GET", "/", FakeHeaders(Seq("User-Agent" -> aDesktopUserAgent)), AnyContentAsEmpty)

      val resultHtml = resolver.resolve(desktopRequest, "/path/to/template1-x.html")
      val resultHtml0 = resolver.resolve0(desktopRequest, "/path/to/template1-x.html")
      assert(resultHtml === "/path/to/template1.html")
      assert(resultHtml0 === "/path/to/template1.html")

      val resultHtm = resolver.resolve(desktopRequest, "/path/to/template1-x.htm")
      val resultHtm0 = resolver.resolve0(desktopRequest, "/path/to/template1-x.htm")
      assert(resultHtm === "/path/to/template1.htm")
      assert(resultHtm0 === "/path/to/template1.htm")


      val resultJs = resolver.resolve(desktopRequest, "/path/to/template1-x.js")
      val resultJs0 = resolver.resolve0(desktopRequest, "/path/to/template1-x.js")
      assert(resultJs === "/path/to/template1-x.js")
      assert(resultJs0 === "/path/to/template1-x.js")
    }

    it("should resolve template when it ends with -x in a request from mobile ") {
      val mobileRequest = FakeRequest("GET", "/", FakeHeaders(Seq("User-Agent" -> aMobileUserAgent)), AnyContentAsEmpty)

      val resultHtml = resolver.resolve(mobileRequest, "/path/to/template1-x.html")
      val resultHtml0 = resolver.resolve0(mobileRequest, "/path/to/template1-x.html")
      assert(resultHtml === "/path/to/template1-m.html")
      assert(resultHtml0 === "/path/to/template1-m.html")

      val resultHtm = resolver.resolve(mobileRequest, "/path/to/template1-x.htm")
      val resultHtm0 = resolver.resolve0(mobileRequest, "/path/to/template1-x.htm")
      assert(resultHtm === "/path/to/template1-m.htm")
      assert(resultHtm0 === "/path/to/template1-m.htm")


      val resultJs = resolver.resolve(mobileRequest, "/path/to/template1-x.js")
      val resultJs0 = resolver.resolve0(mobileRequest, "/path/to/template1-x.js")
      assert(resultJs === "/path/to/template1-x.js")
      assert(resultJs0 === "/path/to/template1-x.js")

    }
  }
}
