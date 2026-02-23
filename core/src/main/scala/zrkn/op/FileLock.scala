package zrkn.op

import java.io.RandomAccessFile

object FileLock:

  def withFileLock[T](taskId: String, lockDir: os.Path = os.pwd, delete: Boolean = true)(task: => T): Option[T] =
    os.makeDir.all(lockDir)
    val lockPath = lockDir / s"$taskId.lock"
    val raf = RandomAccessFile(lockPath.toIO, "rw")
    try
      val channel = raf.getChannel()
      val lock = channel.tryLock()
      if lock != null then
        try
          Some(task)
        finally
          if delete then os.remove(lockPath)
          lock.release()
          channel.close()
      else
        None
    finally
      raf.close()

  def test(size: Int, sleepTime: Int): Unit =
    val tasks = (1 to size).toList
    new Thread(() => tasks.foreach(t => withFileLock(s"task-$t"){
      if !os.exists(os.pwd/f"$t.txt") then
        println(s"[Thread-${Thread.currentThread().getId}] Running task $t.")
        Thread.sleep(sleepTime)
        os.write(os.pwd/f"$t.txt", s"Task $t is done.")
        println(s"[Thread-${Thread.currentThread().getId}] Task $t is done.")
    })).start()
