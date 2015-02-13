package util

import java.net.URLEncoder
import java.util.Date

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
      def stripHTML(post: JsObject,field:String) = {
        val oldBody = (post \ field).as[String]
        post.transform(
          __.json.update((__ \ '_id ).json.put(post \ "_id" \ "$oid")) andThen
            __.json.update((__ \ ("html"+field) ).json.put( JsString( (oldBody)) )) andThen
            __.json.update( (__ \ field ).json.put( JsString(org.jsoup.Jsoup.parse( (post \ field).as[String] ).text) )
            )).get
      }
      def esIndex(post: JsObject,_type : String,id :String)= {
        WS.url(ESUtilities.ESURL+"blox/"+_type+"/"+URLEncoder.encode((post \id).as[String], "UTF-8"))
          .withQueryString("test"->"hg hg")
          .withHeaders("Content-Type"->"application/json;charset=UTF-8")
          .put(post).map(rq => rq.body)
      }

  def esSearch (query:String,_type: String) = {

    println(query)
    val ul:AsyncHttpClient = WS.client.underlying

    val rb = new RequestBuilder().setUrl(ESUtilities.ESURL+"blox/"+_type+"/_search").setBody(
      query)
       .setMethod("GET")
      .setHeader("Content-Type","application/json;charset=UTF-8")
      .build()
    Json.parse(ul.executeRequest(rb).get().getResponseBody)
  }


}
