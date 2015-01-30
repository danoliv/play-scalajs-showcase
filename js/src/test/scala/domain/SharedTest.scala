package domain

import shared.domain.todo._
import utest._

/**
 *
 */

object SharedTest extends TestSuite {

  def tests = TestSuite {

    val todoApi:TodoIntf = null

    "A plan can have a task" - {
      val plan = new Plan
      plan.loadFromHistory(Seq(
        TaskCreated(new Task(Some(1L), "Do this", false)),
        TaskRedefined(1L, "Do this other thing"),
        TaskCompleted(1L)))

      todoApi.clearCompletedTasks

      assert(plan.countLeftToComplete == 0)
    }

    "A plan can have two task" - {
      val plan = new Plan
      plan.loadFromHistory(Seq(
        TaskCreated(new Task(Some(1L), "Do this", false)),
        TaskRedefined(1L, "Do this other thing"),
        TaskCompleted(2L),
        TaskCreated(new Task(Some(2L), "Do this honey", false))))

      todoApi.clearCompletedTasks

      assert(plan.countLeftToComplete == 1)
    }
  }


}