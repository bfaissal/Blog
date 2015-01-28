package util

import controllers.Application._
import play.api.libs.json._
import play.api.libs.ws.WS
import reactivemongo.bson.BSONObjectID
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.Play.current

/**
 * Created by faissalboutaounte on 15-01-25.
 */
object ESUtilities {
      val ESURL = System.getenv(" ES_BONSAI_URLS")
      def esIndex(post: JsObject)= {


        val thePost = post.transform(__.json.update(
          (__ \ 'htmlbody ).json.put( JsString( ( (post \ "body").as[String] )) ) andThen
            (__ \ 'body ).json.put( JsString(org.jsoup.Jsoup.parse( (post \ "body").as[String] ).text) )
        )).get

        println( thePost )
        val res = WS.url(ESUtilities.ESURL+"blox/post/"+(post \"url").as[String])
          .withHeaders("Content-Type"->"application/json;charset=UTF-8")
          .put(thePost).map(rq => rq.body)
        res.map(println(_))

      }
}
