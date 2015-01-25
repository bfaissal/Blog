package controllers

import java.io.{FileInputStream, File}

import com.ning.http.client.{AsyncHttpClient, StringPart, RequestBuilder}
import com.sksamuel.scrimage.Image
import play.api._
import play.api.http.MimeTypes
import play.api.i18n.Messages
import play.api.libs.iteratee.{Enumerator, Iteratee, Enumeratee}
import play.api.libs.json
import play.api.libs.json._
import play.api.libs.oauth.{RequestToken, ConsumerKey, OAuthCalculator}
import play.api.libs.ws.WS
import play.api.mvc._
import play.modules.reactivemongo.MongoController
import play.modules.reactivemongo.json.collection.JSONCollection
import reactivemongo.api.QueryOpts
import reactivemongo.bson.{BSONInteger, BSONObjectID}
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.Play.current


object Application extends Controller with MongoController {
  def collection: JSONCollection = db.collection[JSONCollection]("posts")
  def sequences: JSONCollection = db.collection[JSONCollection]("counters")
  def users: JSONCollection = db.collection[JSONCollection]("users")

  def connect = Action.async(parse.json){ implicit request =>

    //users.find(Json.obj("_id")).cursor[JsObject].headOption.map(f => f.map(Ok("")))
     Future(Ok(""))
  }
  protected val generateId = __.json.update(
    (__ \ '_id \ '$oid).json.put(JsString(BSONObjectID.generate.stringify))
  )
  def index = Action.async {
    val res = collection.find(Json.obj(/*"published" -> true*/)).options(QueryOpts().batchSize(3000)).sort(Json.obj("creationDate" -> -1)).cursor[JsObject].collect[List]()
    res.map(e => {
      Ok(views.html.index(e))
    })
  }
  def postById(id:String) = Action.async {
    collection.find(Json.obj("_id"->Json.obj("$oid"->id))).cursor[JsObject].headOption.map(p => Ok(p.get))
  }
  def post(url:String) = Action.async{
    collection.find(Json.obj(("published" -> true),("url" -> url)))
      .cursor[JsObject].headOption.map( p => Ok(views.html.post(p.getOrElse(Json.obj()))))
  }
  def form(post:String) = Action.async {
      Future.successful(Ok(views.html.form("")))
  }
  def allPosts(f:Option[Long],l:Option[Long]) = Action.async {

    //asOk.chunked(Enumerator("[") andThen (collection.find(Json.obj()).cursor[JsObject].enumerate() &> Enumeratee.map(e => e.toString()+",")) andThen Enumerator("{}]")).as(JSON)
    val query  = f.map({fDate:Long => {Json.obj("creationDate"->Json.obj("$lt"->fDate))}}).getOrElse(
      l.map({lDate:Long => {Json.obj("creationDate"->Json.obj("$gt"->lDate))}}).getOrElse(Json.obj())
    )
    collection.find(query).options(QueryOpts().batchSize(20)).sort(Json.obj("creationDate" -> -1)).cursor[JsObject].collect[List]().map(list => Ok(Json.toJson(list)))
  }


  def savePost = Action.async(parse.json){
    request => {
      val post = ((request.body \ "_id") match {
        case _:JsUndefined =>{
          //sequences.fin
          val connectedUser = Json.obj("fullName"->JsString(Messages("leila")),"_id"->"abid.leila@gmail.com")
          val addAuthor = __.json.update((__ \ 'author ).json.put(connectedUser))
          val creationDate = __.json.update((__ \ 'creationDate ).json.put(JsNumber(new java.util.Date().getTime())))
          request.body.transform(addAuthor andThen generateId andThen creationDate).get
        }
        case _ =>{
          request.body
        }
      }).transform(__.json.update((__ \ 'lastUpdateDate ).json.put(JsNumber(new java.util.Date().getTime())))).get
      collection.save(post)


      Future.successful(Ok(post).as(MimeTypes.JSON))
    }

  }
  def deletePost = Action.async(parse.json){
    request => collection.remove(request.body).map(lastError => Ok(Messages("Successfully_deleted ")+lastError)).recover({case t=> BadRequest(Messages("Delete_incomplete"))})
  }

  def tags(query :String) = Action{

    Ok(Json.obj("data"->Json.arr(Json.obj("tag"->"tag1"),Json.obj("tag"->"tag2"),Json.obj("tag"->"tag3"))))
    Ok(Json.arr(Json.obj("text"->"stag1"),Json.obj("text"->"wtag2"),Json.obj("text"->"tag3")))
  }
  var i =0
  def upload(CKEditorFuncNum:String) = Action(parse.multipartFormData) { request =>
    request.body.file("upload").map { picture =>
      import java.io.File
      i = i+1
      val filename = picture.filename
      val contentType = picture.contentType


      picture.ref.moveTo(new File(s"/tmp2/picture/${filename}_$i"))
      //picture.ref.
      val res = s"<html><body><script type='text/javascript'>window.parent.CKEDITOR.tools.callFunction('$CKEditorFuncNum', '/img/$filename','');</script></body></html>"
      Ok(res).as("text/html")
    }.getOrElse {
      BadRequest(s"<html><body><script type='text/javascript'>alert('${Messages("upload.error.retry")}')</script></body></html>")
    }
  }

  def img(img:String) = Action.async{

    WS.url("http://2.bp.blogspot.com/-aP74Rp910F4/VL0fEtYwWjI/AAAAAAAABeg/RxIRkeV45gk/s1600/une-classe-montessori%2B(1).jpg")
      .getStream().map(
        {case (rh,e) => Ok.chunked(e)}
      )

  }

  def testDropbox = Action.async{

    WS.url("https://api-content.dropbox.com/1/files_put/auto/DSC_1326.JPG")
      .withHeaders("Authorization"->System.getenv("DROPBOX_TOCKEN"))
      .put(new File(s"/Users/faissalboutaounte/Downloads/VirtualBox-4.3.20-96996-OSX.dmg"))
      //.put(new File(s"/tmp2/picture/DSC_1326.JPG"))
       .map( rs => Ok(rs.body))


    //Ok("")
  }


}