package controllers

import java.util.Date

import play.api.libs.json.JsObject

/**
 * Created by faissalboutaounte on 15-02-07.
 */
object TemplateImplicits {
  implicit def lastestPosts : List[JsObject]  = {Application.lastestPosts};
  def formatDate(d:Long):String={
    formatDate(new Date(d))
  }

  def formatDate(d:Date):String={
    val format = new java.text.SimpleDateFormat("EEEE dd MMMM yyyy الساعة HH:mm",new java.util.Locale("ar"))
    format.format(d)
  }

  def trimBody(body:String) = {
    body.substring(0,if(body.length > 1000){1000}else{body.length})
  }
}
