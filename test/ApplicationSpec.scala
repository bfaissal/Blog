import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._

import play.api.test._
import play.api.test.Helpers._

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
@RunWith(classOf[JUnitRunner])
class ApplicationSpec extends Specification {

  "Application" should {

    "send 404 on a bad request" in new WithApplication{
      status(route(FakeRequest(GET, "/boum")).get) must equalTo(NOT_FOUND)
    }

    "render the index page" in new WithApplication(FakeApplication(additionalConfiguration=Map(
      "mongodb.uri"->"mongodb://192.168.59.103:27017/montessori",
      "elasticsearch.url"->"http://localhost:9200/"
    ) )){
      val home = route(FakeRequest(GET, "/")).get

      status(home) must equalTo(OK)
      contentType(home) must beSome.which(_ == "text/html")
      contentAsString(home) must contain ("لا توجد نتائج لهذا البحث")
    }
  }
}
