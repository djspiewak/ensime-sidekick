package com.codecommit
package es

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
  
  // hack
  def unhandled(msg: SExp)
}
