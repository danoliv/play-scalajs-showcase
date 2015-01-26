package shared.config

import shared.Task

import scala.concurrent.Future

trait TodoIntf {

  def all: Future[List[Task]]
  def create(txt: String, done: Boolean) : Future[Either[Task, TodoBusinessException]]
  def update(task: Task): Future[Boolean]
  def delete(id: Long): Future[Boolean]
  def clearCompletedTasks : Future[Boolean]

}
trait TodoException {
  def message: String

  override def toString = message
}

case class TodoBusinessException(message:String) extends Exception(message) with TodoException
case class TodoSystemException(message:String) extends RuntimeException(message) with TodoException

object Routes {

  object Todos {
    val base = "/todos"
    def all = base + "/all"
    def create = base + "/create"
    def update(id: Long) = base + s"/update/$id"
    def delete(id: Long) = base + s"/delete/$id"
    def clear = base + "/clear"
  }

  object Hangman {
    val base = "/hangman"
    def start(level: Int) = base + s"/start/$level"
    def session = base + "/session"
    def guess(g: Char) = base + s"/guess/$g"
    def giveup = base + "/giveup"
  }

  object Chat {
    val base = "/chat"
    def connectSSE(username: String) = base + s"/sse/$username"
    def talk = base + "/talk"
  }
}
