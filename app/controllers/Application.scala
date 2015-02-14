package controllers

import java.io.{FileInputStream, File}
import java.text.SimpleDateFormat

import _root_.util.ESUtilities
import com.ning.http.client.{AsyncHttpClient, StringPart, RequestBuilder}
import com.sksamuel.scrimage.Image
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import play.api._
import play.api.http.MimeTypes
import play.api.i18n.Messages
import play.api.libs.iteratee.{Concurrent, Enumerator, Iteratee, Enumeratee}
import play.api.libs.json
import play.api.libs.json._
import play.api.libs.oauth.{RequestToken, ConsumerKey, OAuthCalculator}
import play.api.libs.ws.WS
import play.api.mvc._
import play.modules.reactivemongo.MongoController
import play.modules.reactivemongo.json.collection.JSONCollection
import reactivemongo.api.QueryOpts
import reactivemongo.bson.{BSONInteger, BSONObjectID}
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.Play.current
import play.api.data._
import scala.concurrent.duration._

import play.api.libs.json.Reads._ // Custom validation helpers
import play.api.libs.functional.syntax._ // Combinator syntax


//
object Application extends Controller with MongoController {
  val PAGE_SIZE = 20;
  val ADS_FREQUENCY = 4;

  def collection: JSONCollection = db.collection[JSONCollection]("posts")
  def drafts: JSONCollection = db.collection[JSONCollection]("drafts")
  def tags: JSONCollection = db.collection[JSONCollection]("tags")
  def sequences: JSONCollection = db.collection[JSONCollection]("counters")
  def users: JSONCollection = db.collection[JSONCollection]("users")

  def setDate(name: String) = __.json.update((__ \ name ).json.put(JsNumber(new java.util.Date().getTime())))

  def connect = Action.async(parse.json){ implicit request =>

    //users.find(Json.obj("_id")).cursor[JsObject].headOption.map(f => f.map(Ok("")))
     Future(Ok(""))
  }
  protected val generateId = __.json.update(
    (__ \ '_id \ '$oid).json.put(JsString(BSONObjectID.generate.stringify))
  )
  def index = Action.async {
    val res = collection.find(Json.obj("published" -> "true")).options(QueryOpts().batchSize(20)).sort(Json.obj("creationDate" -> -1)).cursor[JsObject].collect[List]()
    res.map(e => {
      Ok(views.html.index(e))
    })
  }

  def lastestPosts =  {
    val res = collection.find(Json.obj("published" -> true),Json.obj("title"->"1","url"->"2","cover"->"3")).options(QueryOpts().batchSize(3)).sort(Json.obj("creationDate" -> -1)).cursor[JsObject].collect[List]()
    val trans = (__ \ 'cover ).json.update(of[JsString].map{
      case JsString(cover) => JsString(cover)
      case _ => JsString("http://localhost:9000/img/1422940618442884000?size=m")
    })

    val modified = res.map({
      r => r.take(3).map({
        e => e.transform({
          __.json.update(
          (__ \ 'cover ).json.put({
            e.transform( (__ \ 'cover).json.pick) match {
                case error:JsError => JsString("/assets/images/placeholder.png")
                case s:JsSuccess[JsString] => s.get
            }
          })
          )
        }).get
      })
    })


    //__.json.update()
    Await.result(modified,60 seconds)
  }

  def executeESSearch(query: String, _type: String = "post", isSearch: Boolean = false,oQuery:String = "")(implicit request: Request[AnyContent]) = {
    val restTemp = ESUtilities.esSearch(query, _type)
    var index = 0;
    def incremt: Int = {
      index = index + 1
      index
    }

    val totalResult = (restTemp\"hits"\"total").as[Int];
    println("totalResult = "+totalResult)
    val rest = restTemp.transform(
      (__ \ "hits" \ "hits").json.update(
        __.read[JsArray].map {
          case JsArray(a) => {
            JsArray(a.map(e => {
              e.transform(
                __.json.update(
                  (__ \ "showAdds").json.put(
                    JsBoolean({val currentIndex  = incremt;((currentIndex % ADS_FREQUENCY) == 0) || ((currentIndex % totalResult) ==0)})
                  )
                )
              ).get
            }))
          }
        }
      )
    ).get
    val pages = (totalResult / PAGE_SIZE) + (if ((totalResult % PAGE_SIZE) > 0) 1 else 0)

    Future(Ok(views.html.search(Json.obj("results" -> rest.transform((__ \ 'hits).json.pick).get), isSearch, pages,oQuery,totalResult)(request)))

  }


  def indexES(page:Option[Int]) = Action.async {
    implicit request =>{

    executeESSearch( s"""
      {
        "from" : ${PAGE_SIZE * (page.getOrElse(1) - 1)}, "size" : $PAGE_SIZE,
        "sort" : [
            { "creationDate" : { "order": "desc" }}
        ],
        "query": {
            "filtered" : {
                "filter" : {
                    "term" : {
                       "published" : true
                    }
                }
            }
        }
      }"""
    )
  }

  }
  //"""
  def tagsSearch(tag:String,page:Option[Int]) = Action.async{
    implicit request =>{
    executeESSearch(
      s"""
        |{
        |    "from" : ${PAGE_SIZE*(page.getOrElse(1)-1)}, "size" : $PAGE_SIZE,
        |    "sort" : [{ "creationDate" : { "order": "desc" }}],
        |    "query": {
        |            "filtered" : {
        |                "filter" : {
        |                    "term" : {
        |                       "published" : true
        |                    }
        |                },
        |    "query" : {
        |        "match" :  { "tags.text" : "$tag" }
        |
        |    }
        |    }
        |    }
        |}
      """.stripMargin,"post")
      }
  }

  def relatedPost(tags:JsValue) = {
    val should = (tags\"tags") match {
      case JsArray(a) => {
        a.map(e => {
          s"""
             |  { "match": { "tags.text": "${(e\"text").as[String]}"}}
             """.stripMargin
        }).mkString("[",",","]")
      }
      case _ => ""
    }
    val query =
        s"""
          |{
          | size:5,
          |
          | "filter" : {
                "term" : {
                  "published" : true
                }
          |  },
          |  "query": {
          |    "bool": {
          |      "should":  $should
          |    }
          |  }
          |}
        """.stripMargin
    (ESUtilities.esSearch(query, "post")\"hits"\"hits") match
    {
      case JsArray(a)=> a.toList.filter(e =>{!(e\"_id").equals((tags\"_id")) })
      case _ => List[JsValue]()
    }

  }
  //"""
   implicit def  tagsAggregation = {
     val res = WS.url(ESUtilities.ESURL+"blox/post/_search").post(
       """
         |{
         |    "aggs" : {
         |        "tags" : {
         |            "terms" : { "field" : "tags.text" }
         |        }
         |    },
         |    "size": 0
         |}
       """.stripMargin).map(rh => Json.obj("allTags"->Json.parse(rh.body)))
       Await.result(res,10 seconds)

  }
  //"""

  def postById(id:String) = Action.async {
      collection.find(Json.obj("_id"->Json.obj("$oid"->id))).cursor[JsObject].headOption.map(p => Ok(p.get))
  }
  def getComments(url: String) = Action.async{
    collection.find(Json.obj("url" -> url),Json.obj("comments"->"1")).cursor[JsObject].headOption.map(list => Ok(Json.toJson(list.getOrElse(Json.obj()))))
  }
  def comment(url: String) = Action.async(parse.json){
    request => {
      val validationRead = (
        (__ \ "name").read[String](minLength[String](2)  andKeep maxLength[String](50) ) and
          (__ \ "email").read[String](minLength[String](2)  andKeep  maxLength[String](50) andKeep email) and
          (__ \ "comment").read[String](minLength[String](2) andKeep maxLength[String](2000))
        tupled
        )

      request.body.validate(validationRead) match {
        case s: JsSuccess[String] => {
          WS.url("https://www.google.com/recaptcha/api/siteverify")
            .withQueryString(("secret"->System.getenv("RECAPTCHA_KEY"))
                ,("response"->(request.body \ "recaptcha").as[String])).get().map(rh => {
            val newComment  = Json.parse(rh.body);
            if((newComment\"success").as[Boolean]) {

              val transformedComment = request.body.transform(__.json.update((__ \ 'recaptcha).json.prune) andThen setDate("date")).get
              collection.update(Json.obj("url" -> url),Json.obj("$push"->Json.obj("comments"->  transformedComment )))

              Ok(transformedComment).as(JSON)
            } else BadRequest(Messages("badCaptcha"))
          })}
        case e: JsError => Future(BadRequest(Messages("VerifyInputes")))
      }
    }
  }

  def post(url:String) = Action.async {
    collection.find(Json.obj(("published" -> true), ("url" -> url)))
      .cursor[JsObject].headOption.map(p => {
      p match {
        case None => Redirect("/search", Map("query" -> Seq(url))) //Ok(views.html.post(Json.obj()))
        case Some(value) => Ok(views.html.post(value))
      }
      //Ok(views.html.post(p.getOrElse(Json.obj())))
    })


  }

  def apost(url:String) = Action{
    val restTemp = ESUtilities.esSearch(
      s"""
      {
        "size" : 1,
        "query": {
            "filtered" : {
                "filter" : {
                    "term" : {
                       "published" : true
                    }
                },
                "query": {
                   "multi_match": {
                      "fields" : ["url"],
                      "query":  "${java.net.URLDecoder.decode(url, "UTF-8")}"
                   }
                }
            }
        }
        }
      }
      """.stripMargin
      , "post")
    var index = 0;
    println(restTemp)
    val rest = restTemp.transform(
      ((__ \ "hits" \ "hits")).json.pick
    ) match {
      case s:JsSuccess[JsValue] => s.get match {
        case JsArray(a) => {
          a(0).transform(
            __.json.update((__ \ "_source" \ "body" ).json.put(a(0) \ "_source" \ "htmlbody")) andThen
            (__ \ "_source").json.pick)
          match {
            case o: JsSuccess[JsObject] => o.get
            case _ => Json.obj()
          }
      }

        case _  => Json.obj()
      }
      case e:JsError=> println(e.errors.mkString);Json.obj()
    }

    Ok(views.html.post(rest))




  }
  def preview = Action {
    request => {
      Ok(views.html.post(Json.parse(request.body.asFormUrlEncoded.get("preview").mkString).transform(__.json.update((__ \ 'lastUpdateDate ).json.put(JsNumber(new java.util.Date().getTime())))).get))
    }

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

  def saveDraft = Action(parse.json){
      request => {
        drafts.save(request.body)
        Ok("")
      }
  }

  def savePost(draft:Boolean) = Action.async(parse.json){
    request => {
      val post = ((request.body \ "_id") match {
        case _:JsUndefined =>{
          request.body.transform(
              __.json.update((__ \ 'author ).json.put(
                  Json.obj("fullName"->JsString(Messages("leila")),"_id"->"abid.leila@gmail.com"))
                  ) andThen
              __.json.update((__ \ 'published ).json.put(JsBoolean(false))) andThen
              __.json.update((__ \ 'creationDate ).json.put(JsNumber(new java.util.Date().getTime()))) andThen
              __.json.update((__ \ 'url).json.copyFrom((__ \ 'title).json.pick.map({case JsString(e)=> JsString(e.replaceAll(" ","-"))})))  andThen
                generateId
          ).get
        }
        case _ =>{
          request.body
        }
      }).transform(__.json.update((__ \ 'lastUpdateDate ).json.put(JsNumber(new java.util.Date().getTime())))).get
      collection.save(post)
      if(!draft){
        ESUtilities.esIndex(ESUtilities.stripHTML(post,"body"),"post","_id")
      }

      (post\"tags") match {
        case list:JsUndefined => {}
        case list => list.as[Array[JsObject]].foreach(e => ESUtilities.esIndex(e,"tags","text"))
      }

        //.as[Array[JsObject]]

      Future.successful(Ok(post).as(MimeTypes.JSON))
    }

  }
  def deletePost = Action.async(parse.json){
    request => collection.remove(request.body).map(lastError => Ok(Messages("Successfully_deleted ")+lastError)).recover({case t=> BadRequest(Messages("Delete_incomplete"))})
  }

  def addtags = Action{
    request => {
      request.body.asJson.map(tag => {
        tags.save(tag)
      })
      Ok("")
    }
  }
  def tags(query :String) = Action{
    val res = ESUtilities.esSearch(
      s"""
        |{
        |    "query" : {
        |        "wildcard" :  { "text" : "*$query*" }
        |    }
        |}
      """.stripMargin,"tags") \"hits"\"hits"\\"_source"

    Ok(Json.obj("data"->Json.arr(Json.obj("tag"->"tag1"),Json.obj("tag"->"tag2"),Json.obj("tag"->"tag3"))))
    Ok(Json.obj("co"->res)\"co")
  }

  def upload(CKEditorFuncNum:String) = Action.async(parse.multipartFormData) { request =>
    request.body.file("upload").map { picture =>
      import java.io.File

      val filename = System.nanoTime()


      val tempFile= File.createTempFile("upload","tmp")
      picture.ref.moveTo(tempFile,true)//new File(s"/tmp2/picture/${filename}_$i"))

      WS.url(s"https://api-content.dropbox.com/1/files_put/auto/$filename.JPG")
        .withHeaders("Authorization"->System.getenv("DROPBOX_TOCKEN"))
        .put(tempFile)
        .map( rs => {
            tempFile.delete()
            val res = s"<html><body><script type='text/javascript'>window.parent.CKEDITOR.tools.callFunction('$CKEditorFuncNum', '/img/$filename?size=l','');window.parent.ARABICM.addImage('/img/$filename');</script></body></html>"
            Ok(res).as("text/html")
        })

    }.getOrElse {
      Future.successful(BadRequest(s"<html><body><script type='text/javascript'>alert('${Messages("upload.error.retry")}')</script></body></html>"))
    }
  }

  def img(img:String,size:Option[String]=Some("l")) = Action{
    val enumerator = Concurrent.unicast[Array[Byte]](c => {
      WS.url(s"https://api-content.dropbox.com/1/thumbnails/auto/$img.JPG?size=${size.getOrElse("l")}&format=jpeg")
        .withHeaders("Authorization"->System.getenv("DROPBOX_TOCKEN"))
        .get(rh => {Iteratee.foreach(e=> c.push(e));}).onComplete({case f => c.end()})
    })
    Ok.chunked(enumerator andThen Enumerator.eof).as("image/jpeg")
    /*println("downloading ...")
    //WS.url(s"https://api-content.dropbox.com/1/thumbnails/auto/$img.JPG?size=${size.getOrElse("l")}&format=jpeg")
    WS.url(s"https://api-content.dropbox.com/1/files/auto/$img.JPG")
      .withHeaders("Authorization"->System.getenv("DROPBOX_TOCKEN"))
      .get//(rh => Iteratee.foreach(e=> ))

      .map(
        {rq => {
          println("=======:::> "+rq.body)
          rq.allHeaders.filter({case (k,v) => !"content-security-policy-report-only".equals(k)}).map({case (k,v)=>{(k,v.mkString)}}).foldLeft(Ok(rq.body))({case (rp,h)=> rp.withHeaders(h)})

        }}
      )   */

  }

  def testDropbox = Action.async{

    WS.url("https://api-content.dropbox.com/1/files_put/auto/DSC_1326.JPG")
      .withHeaders("Authorization"->System.getenv("DROPBOX_TOCKEN"))
      .put(new File(s"/Users/faissalboutaounte/Downloads/VirtualBox-4.3.20-96996-OSX.dmg"))
      //.put(new File(s"/tmp2/picture/DSC_1326.JPG"))
       .map( rs => Ok(rs.body))


    //Ok("")
  }

  def indexing =Action.async{
    val settings =
      """
        {
        |"settings": {
        |    "analysis": {
        |          "filter": {
        |            "arabic_stop": {
        |              "type": "stop",
        |              "stopwords": "_arabic_"
        |            },
        |            "arabic_stemmer": {
        |              "type": "stemmer",
        |              "language": "arabic"
        |            }
        |          },
        |          "analyzer": {
        |            "my_arabic": {
        |              "char_filter": [
        |                "html_strip"
        |              ],
        |              "filter": [
        |                "lowercase",
        |                "arabic_stop",
        |                "arabic_normalization",
        |                "arabic_stemmer"
        |              ],
        |              "tokenizer": "standard"
        |            }
        |          }
        |        }},
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
               "url" : {
                          "type" : "string",
                          "index" : "not_analyzed"
                        },
                "tags" : {
                    "properties" : {
                      "text" : {
                        "type" : "string",
                        "index" : "not_analyzed"
                      }
                    }
                },
         "suggest" : {


                                "type": "completion",
                       "index_analyzer": "simple",
                       "search_analyzer": "simple",
                       "payloads": false


                        },
                "creationDate" : {
                    "type" : "date"
                }


              }
            }
          }
        }
      """.stripMargin

    for(
     post <- collection.find(Json.obj(("url" -> "testxxx")))
    .cursor[JsObject].headOption.map( p => p.getOrElse(Json.obj()));
      r1 <- WS.url(ESUtilities.ESURL+"blox").put(settings).map(rq => { rq.body})) yield Ok(r1+" ----- "+" ----- ")

  }
  def analyse(text:String) =Action.async{
    val text = collection.find(Json.obj(("url" -> "testxxx")))
      .cursor[JsObject].headOption.map( p => {p.map(px => (px\"body").as[String]).getOrElse("")})
    text.map(aPost => {
      val ul:AsyncHttpClient = WS.client.underlying
      val rb = new RequestBuilder().setUrl("http://localhost:9200/blox/_analyze?analyzer=arabic").setBody(
        aPost.replace("<p>","").replace("</p>","")

      ).setHeader("Content-Type","text/html;charset=UTF-8").setMethod("GET").build()
      Ok(ul.executeRequest(rb).get().getResponseBody)
    })



  }

  def search(search:String,page:Option[Int]) =Action.async{
    implicit request => {
      executeESSearch(
        s"""
      {
        "from" : ${PAGE_SIZE * (page.getOrElse(1) - 1)}, "size" : $PAGE_SIZE,
        "query": {
            "filtered" : {
                "filter" : {
                    "term" : {
                       "published" : true
                    }
                },
                "query": {
                   "multi_match": {
                      "fields" : ["title","body"],
                      "query":  "$search"
                   }
                }
            }
        },
        "highlight" : {
            "fields" : {
                "body" : {},
                "title" : {}
            }
        }
      }
      """.stripMargin, "post", true,search)
    }
  }


  def migration = Action.async{
    val sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX")
    import scala.collection.JavaConversions._
    val res = WS.url(s"https://www.googleapis.com/blogger/v3/blogs/6365368560867867924/posts?key=${System.getenv("GOOOGLE_KEY")}&maxResults=100")
      .get.map(r => {

        (Json.parse(r.body)\"items").transform[JsArray](of[JsArray].map({ case JsArray(se) => {
          JsArray(se.map( e => {

            val doc = org.jsoup.Jsoup.parse((e \ "content").as[String].replaceAll("Arial,","'Noto Naskh Arabic',"))
            Json.obj("body"-> e \ "content",
              "title"-> e \ "title",
              "body"-> (e \ "content").as[String].replaceAll("Arial,","'Noto Naskh Arabic',"),
              "imgs" -> doc.select("img[src]").map( {case img:Element => {
                  JsString(img.attr("src"))
                  }}).foldLeft[Seq[JsString]](Seq[JsString]())({case (lx,ex) => {lx.+:(ex)}}) ,
              "url"-> (e \ "url").as[String].replace("http://www.arabicmontessori.com/","")  ,
              "creationDate" -> sdf.parse((e \ "published").as[String]).getTime,
              "tags" ->  {val xsr = (e \ "labels").transform[JsArray](of[JsArray].map({case JsArray(tags) => JsArray(tags.map(tag => Json.obj("text" -> tag)))})).getOrElse(JsArray(Seq()));xsr}
            )
          }))
        }})).get

    })

    res.map({case JsArray(s) => {s.map({
      e =>

        val post = ((e \ "_id") match {
          case _:JsUndefined =>{
            e.transform(
                __.json.update((__ \ 'author ).json.put(
                    Json.obj("fullName"->JsString(Messages("leila")),"_id"->"abid.leila@gmail.com")
                )) andThen
                __.json.update((__ \ 'published ).json.put(JsBoolean(true)))
            andThen generateId
            ).get
          }
          case _ => e
        }).transform(__.json.update((__ \ 'lastUpdateDate ).json.put(JsNumber(new java.util.Date().getTime())))).get

        collection.save(post)
        ESUtilities.esIndex(ESUtilities.stripHTML(post,"body"),"post","_id")

        (post\"tags") match {
          case list:JsUndefined => {}
          case list => {list.as[Array[JsObject]].foreach(e => { (e\"text") match {
            case xsd:JsUndefined => {}
            case _ => ESUtilities.esIndex(e,"tags","text")
          }})}
        }

    })
      Ok(" Ok ")
    }})

  }

  def gone(blog:String) = Action{
    Ok("Salam "+blog)
  }

}
