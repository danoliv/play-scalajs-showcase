package example

import org.scalajs.dom
import org.scalajs.dom.ext.Ajax
import rx._
import shared.config.Routes
import shared.domain.todo._

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js.annotation.JSExport
import scala.util.{Failure, Success}
import scalatags.JsDom.all._
import scalatags.JsDom.tags2.section

import org.scalajs.jquery.{jQuery => $}
import scala.scalajs.js.Dynamic.{global => g}
import upickle._

@JSExport
object TodoJS {

  import common.Framework._

  object Model {


    val tasks = Var(List.empty[Task])

    val done = Rx {
      tasks().count(_.done)
    }

    val notDone = Rx {
      tasks().length - done()
    }

    val editing = Var[Option[Task]](None)

    val filter = Var("All")

    val filters = Map[String, Task => Boolean](
      ("All", t => true),
      ("Active", !_.done),
      ("Completed", _.done)
    )

    def init: Future[Unit] = {
      Ajax.get(Routes.Todos.all).map { r =>
        read[List[Task]](r.responseText)
      }.map { r =>
        tasks() = r
      }
    }

    /**
     *
     */
    def all: Future[List[Task]] = TodoClient.allScheduled

    /**
     *
     */
    def create(txt: String, done: Boolean = false) = {

      TodoClient.scheduleNew(txt).onComplete {

        case Success(result) =>
          if (result.v.isLeft) {
            //@todo implement tasks() = result.left.get +: tasks()
          }
          else {
            dom.alert(result.ex.message)
          }
        case Failure(e) => dom.alert("create failed: " + e.getMessage)
      }
    }

    /**
     *
     */
    def update(task: Task) = {
      //@todo implement
      TodoClient.redefine(task.id, task.txt).onComplete {

        case Success(_) =>
          val pos = tasks().indexWhere(t => t.id == task.id)
          tasks() = tasks().updated(pos, task)

        //case Success(false) => dom.alert("update failed")
        case Failure(e) => dom.alert("update failed: " + e.toString)
      }
    }

    /**
     *
     */
    def delete(taskId: TaskId) = {
      TodoClient.cancel(taskId).onComplete {

        case Success(_) =>
          tasks() = tasks().filter(_.id != taskId)

        //case Success(false) => dom.alert("delete failed")
        case Failure(e) => dom.alert("delete failed: " + e.getMessage)
      }
    }

    /**
     *
     */
    def clearCompletedTasks() = {
      TodoClient.clearCompletedTasks.onComplete {

        case Success(history) =>
          if (history == null) {
            dom.alert("clearCompletedTasks failed")
          } else {
            //@todo implement plan.loadFromHistory(history)
            tasks() = tasks().filter(!_.done)
          }
        //case Success(null) => dom.alert("clearCompletedTasks failed")
        case Failure(e) => dom.alert("clearCompletedTasks failed: " + e.getMessage)
      }
    }

  }

  val inputBox = input(
    id := "new-todo",
    placeholder := "What needs to be done?",
    autofocus := true
  ).render

  def templateHeader = {
    header(id := "header")(
      form(
        inputBox,
        onsubmit := { () =>
          Model.create(inputBox.value)
          inputBox.value = ""
          false
        }
      )
    )
  }

  def templateBody = {
    section(id := "main")(
      input(
        id := "toggle-all",
        `type` := "checkbox",
        cursor := "pointer",
        onclick := { () =>
          val target = Model.tasks().exists(_.done == false)
          //          Var.set(tasks().map(_.done -> target): _*)
        }
      ),
      label(`for` := "toggle-all", "Mark all as complete"),
      partList,
      partControls
    )
  }

  def templateFooter = {
    footer(id := "info")(
      p("Double-click to edit a todo"),
      p("Original version created by ", a(href := "https://github.com/lihaoyi/workbench-example-app/blob/todomvc/src/main/scala/example/ScalaJSExample.scala")("Li Haoyi")),
      p("Modified version with database backend can be found ", a(href := "https://github.com/hussachai/play-scalajs-showcase")("here"))
    )
  }

  def partList = Rx {
    ul(id := "todo-list")(
      for (task <- Model.tasks() if Model.filters(Model.filter())(task)) yield {
        val inputRef = input(`class` := "edit", value := task.txt).render

        li(
          `class` := Rx {
            if (task.done) "completed"
            else if (Model.editing() == Some(task)) "editing"
            else ""
          },
          div(`class` := "view")(
            "ondblclick".attr := { () =>
              Model.editing() = Some(task)
            },
            input(`class` := "toggle", `type` := "checkbox", cursor := "pointer", onchange := { () =>
              Model.update(task.copy(done = !task.done))
            }, if (task.done) checked := true else ""
            ),
            label(task.txt),
            button(
              `class` := "destroy",
              cursor := "pointer",
              onclick := { () => Model.delete(task.id)}
            )
          ),
          form(
            onsubmit := { () =>
              Model.update(task.copy(txt = inputRef.value))
              Model.editing() = None
              false
            },
            inputRef
          )
        )
      }
    )
  }

  def partControls = {
    footer(id := "footer")(
      span(id := "todo-count")(strong(Model.notDone), " item left"),
      ul(id := "filters")(
        for ((name, pred) <- Model.filters.toSeq) yield {
          li(a(
            `class` := Rx {
              if (name == Model.filter()) "selected"
              else ""
            },
            name,
            href := "#",
            onclick := { () => Model.filter() = name}
          ))
        }
      ),
      button(
        id := "clear-completed",
        onclick := { () => Model.clearCompletedTasks},
        "Clear completed (", Model.done, ")"
      )
    )
  }

  @JSExport
  def main(): Unit = {

    Model.init.map { r =>
      dom.document.getElementById("content").appendChild(
        section(id := "todoapp")(
          templateHeader,
          templateBody,
          templateFooter
        ).render
      )
    }
  }


}