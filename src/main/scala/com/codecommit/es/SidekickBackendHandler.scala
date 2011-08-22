package com.codecommit
package es

import org.gjt.sp.jedit.View

import client._
import util._

class SidekickBackendHandler extends BackendHandler {
  import EnsimeProtocol._
  
  var views = Set[View]()
  
  def backgroundMessage(msg: String) {
    viewMessage("ENSIME: " + msg)
  }
  
  def clearAll() {}
  
  def compilerReady() {
    viewMessage("ENSIME: Compiler ready")
  }
  
  def fullTypecheckFinished() {
    viewMessage("ENSIME: Full typecheck finished")
  }
  
  def indexerReady() {
    viewMessage("ENSIME: Indexer ready")
  }
  
  def error(note: Note) {}
  
  // hack
  def unhandled(msg: SExp) {}
  
  private def viewMessage(msg: String) {
    views foreach { _.getStatus.setMessage(msg) }
  }
}
