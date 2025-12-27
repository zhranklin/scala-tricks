package zrkn.op

import io.reactivex.rxjava3.subjects.PublishSubject

import java.util.concurrent.TimeUnit
import scala.jdk.CollectionConverters.*

case class Task[+T](id: String, task: T)
class TaskBatcher[T, R](name: String, size: Int, batchTimeWindow: Int, executionTimeout: Int)(processor: List[Task[T]] => Task[T] => R):
  val results = scala.collection.concurrent.TrieMap[String, R | Exception]()
  val myTasks = PublishSubject.create[Task[T]]()
  new Thread((() => {
    println(s"start TaskBatcher thread $name")
    myTasks
      .buffer(batchTimeWindow, TimeUnit.SECONDS, size)
      .subscribe: ts =>
        val tasks = ts.asScala.toList
        if tasks.nonEmpty then
          new Thread ((() => {
            try
              println(s"executing Batch of $name: ${tasks.map(_.task)}")
              val resultF = processor(tasks)
              tasks.foreach: t =>
                try
                  results.put(t.id, resultF(t))
                catch
                  case e: Exception => results.put(t.id, e)
            catch
              case e: Exception =>
                tasks.foreach(t => results.put(t.id, e))
          }): Runnable).start()
  }): Runnable).start()
  def apply(input: T): R =
    val id = java.util.UUID.randomUUID().toString
    myTasks.onNext(Task(id, input))
    val timeout = System.currentTimeMillis() + executionTimeout * 1000
    while System.currentTimeMillis() < timeout do
      if results.contains(id) then
        results.remove(id).get match
          case e: Exception => throw new Exception(s"failed to execute task $input", e)
          case r: R => return r
      Thread.sleep(50)
    throw new Exception(s"execution timeout for $input")
