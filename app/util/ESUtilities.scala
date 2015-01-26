package util

import controllers.Application._
import play.api.libs.json.{Json, JsObject}
import play.api.libs.ws.WS
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.Play.current

/**
 * Created by faissalboutaounte on 15-01-25.
 */
object ESUtilities {
      def esIndex(post: JsObject)= {

        val settings =
          """
        {
          "mappings": {
            "post" : {
              "properties" : {
                "title" : {
                  "type" : "string",
                  "analyzer": "arabic"
                },
                "body" : {
                          "type" : "string",
                          "analyzer": "arabic"
                        },
                "tag" : {
                  "type" : "string",
                  "index" : "not_analyzed"
                }
              }
            }
          }
        }
          """
        WS.url("http://localhost:9200/blox/post/"+(post \"url").as[String])
          .withHeaders("Content-Type"->"application/json;charset=UTF-8")
          .put(post).map(rq => rq.body)

      }
}
