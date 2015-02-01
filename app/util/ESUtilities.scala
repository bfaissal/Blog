package util

import com.ning.http.client.{RequestBuilder, AsyncHttpClient}

import play.api.libs.json._
import play.api.libs.ws.WS
import reactivemongo.bson.BSONObjectID
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.Play.current

/**
 * Created by faissalboutaounte on 15-01-25.
 */
object ESUtilities {
      val ESURL = System.getenv("ES_BONSAI_URLS")
      val PAGE_SIZE = 2;
      def esIndex(post: JsObject)= {

        val oldBody = (post \ "body").as[String]
        org.jsoup.Jsoup.parse("")
        val thePost = post.transform(
            (__ \ '_id ).json.prune andThen
            __.json.update((__ \ 'htmlbody ).json.put( JsString( (oldBody)) )) andThen
            __.json.update( (__ \ 'body ).json.put( JsString(org.jsoup.Jsoup.parse( (post \ "body").as[String] ).text) )
        )).get

        println( thePost )
        val res = WS.url(ESUtilities.ESURL+"blox/post/"+(post \"url").as[String])
          .withHeaders("Content-Type"->"application/json;charset=UTF-8")
          .put(thePost).map(rq => rq.body)
        res.map(println(_))

      }

  def esSearch (query:String) = {
    val ul:AsyncHttpClient = WS.client.underlying
    println("====>>> "+ESUtilities.ESURL)
    val rb = new RequestBuilder().setUrl(ESUtilities.ESURL+"blox/_search").setBody(
      query)
      .setHeader("Content-Type","text/html;charset=UTF-8").setMethod("GET").build()
    Json.parse(ul.executeRequest(rb).get().getResponseBody)
  }
}
