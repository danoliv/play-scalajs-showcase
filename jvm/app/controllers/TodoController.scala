package controllers

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._
import play.api.mvc._
import shared.domain.todo._
import upickle._

import scala.concurrent.Future


/**
 *
 */
object TodoController extends Controller {

  implicit val jsonReader = (
    (__ \ 'txt).read[String](minLength[String](2)) and
      (__ \ 'done).read[Boolean]
    ).tupled

  def index = Action { implicit request =>
    Ok(views.html.todo("TODO"))
  }

  /**
   *
   */
  def all = Action.async { implicit request =>
    // @nick Delegate to implementation of shared API
    TodoServer.allScheduled.map { r =>
      Ok(write(r))
    }.recover {
      case err => InternalServerError(err.getMessage)
    }
  }

  /**
   *
   */
  def create = Action.async(parse.json) { implicit request =>
    val fn = (txt: String, done: Boolean) =>

      // @nick Delegate to implementation of shared API
      TodoServer.scheduleNew(txt).map { r =>
        Ok(write(r))
      }
    executeRequest(fn)
  }

  /**
   *
   */
  def update(id: Long) = Action.async(parse.json) { implicit request =>
    val fn = (txt: String, done: Boolean) =>

      // @todo: implement (remove CRUd API)
      TodoServer.redefine(TaskId(id), txt).map { r =>
        if (r != null)
          Ok(write(r))
        else
          BadRequest
      }.recover {
        case e => InternalServerError(e)
      }
    executeRequest(fn)
  }

  /**
   *
   */
  def delete(id: Long) = Action.async { implicit request =>

    // @nick Delegate to implementation of shared API
    TodoServer.cancel(TaskId(id)).map { r =>
      if (r.value)
        Ok(write(r))
      else
        BadRequest
    }.recover {
      case e => InternalServerError(e)
    }
  }

  /**
   *
   */
  def clear = Action.async { implicit request =>

    // @nick Delegate to implementation of shared API
    TodoServer.clearCompletedTasks.map { r =>
      Ok(write(r))
    }.recover {
      case e => InternalServerError(e)
    }
  }

  def executeRequest(fn: (String, Boolean) => Future[Result])
                    (implicit request: Request[JsValue]) = {
    request.body.validate[(String, Boolean)].map {
      case (txt, done) => {
        fn(txt, done)
      }
    }.recoverTotal {
      e => Future(BadRequest(e))
    }
  }

}
