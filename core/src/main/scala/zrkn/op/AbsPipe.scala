package zrkn.op

import os.ProcessOutput

abstract class AbsPipe:
  import Pipe.SubProc

  protected var __prev: Option[AbsPipe] = None
  protected var __stdout: ProcessOutput = os.Pipe
  protected var __stderr: ProcessOutput = os.Inherit
  protected def __setPrev(prev: AbsPipe): this.type =
    __prev = Some(prev)
    this

  def |[T <: AbsPipe](next: T): T = next.__setPrev(this)
  val __ : Pipe.Ext[? <: AbsPipe]
  protected def __invokePrev: SubProc = __prev.map(_.__.spawn).getOrElse(SubProc(os.Pipe, () => ()))
