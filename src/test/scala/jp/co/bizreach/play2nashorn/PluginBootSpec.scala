package jp.co.bizreach.play2nashorn

import org.scalatest.{Matchers, FunSpec}
import play.api.Logger
import play.api.test.FakeRequest
import play.test.Helpers
import play.twirl.api.HtmlFormat


class PluginBootSpec extends FunSpec with Matchers with FakePlayHelper {

  describe("Nashorn") {
    it("should evaluate files with a dummy context") {
      runApp(PlayApp()) { app =>
        Logger.info("Booted !")
      }
    }

    it("should render a file when a request comes") {
      runApp(PlayApp()) { app =>
        implicit val request = FakeRequest(Helpers.GET, "/templates/mustache/mustache-template-1")

        val expected = HtmlFormat.raw("<h1>Hello, Mustache !</h1>")
        val response = Mustache.sync("mustache-template-1", """{"key1":"value1","key2":12345}""")

        assert(expected.body === response.body)
      }
    }
  }
}
