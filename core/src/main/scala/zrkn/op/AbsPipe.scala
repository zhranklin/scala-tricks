package zrkn.op

import os.{Path, ProcessOutput}

abstract class AbsPipe:
  import Pipe.SubProc

  protected var __prev: Option[AbsPipe] = None
  protected var __processOutput: ProcessOutput = os.Pipe
  protected def __setPrev(prev: AbsPipe): this.type =
    __prev = Some(prev)
    this

  def __setOutput(output: ProcessOutput): this.type =
    __processOutput = output
    this

  def |[T <: AbsPipe](next: T): T = next.__setPrev(this)
  val __ : Pipe.Ext[? <: AbsPipe]
  protected def __invokePrev(using Path): SubProc = __prev.map(_.__.spawn).getOrElse(SubProc(os.Pipe, () => ()))
