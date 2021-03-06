import controllers.Application
import play.api._
import play.api.mvc._
import play.api.Play.current
import play.filters.gzip.GzipFilter

import scala.concurrent.Future

/**
 * Created by faissalboutaounte on 14-12-29.
 */
object Global extends WithFilters(new GzipFilter()) with GlobalSettings {
  override def onStart(app: Application) {
    Logger.info("Application has started")
  }
  override def onError(request: RequestHeader, ex: Throwable) = {
    if(Play.isDev){
      super.onError(request,ex)
    }
    else {
      Future.successful(Application.InternalServerError(
        views.html.error(500)
      ))
    }
  }
  override def onHandlerNotFound(request: RequestHeader) = {
    Future.successful(Application.NotFound(
      views.html.error(404)
    ))
  }
}
