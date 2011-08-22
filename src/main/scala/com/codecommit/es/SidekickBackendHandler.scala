package com.codecommit
package es

import client._
import util._

class SidekickBackendHandler extends BackendHandler {
  import EnsimeProtocol._
  
  def backgroundMessage(msg: String) {}
  
  def clearAll() {}
  
  def compilerReady() {}
  
  def fullTypecheckFinished() {}
  
  def indexerReady() {}
  
  def error(note: Note) {}
  
  // hack
  def unhandled(msg: SExp) {}
}
