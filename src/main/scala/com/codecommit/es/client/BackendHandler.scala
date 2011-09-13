package com.codecommit
package es
package client

import util.SExp

// TODO
trait BackendHandler {
  import EnsimeProtocol._
  
  def backgroundMessage(msg: String)
  
  def clearAll()
  
  def compilerReady()
  
  def fullTypecheckFinished()
  
  def indexerReady()
  
  def error(note: Note)
  def warning(note: Note)
  
  def ensimeError(code: Int, detail: String)
  def unhandled(msg: SExp)
}
