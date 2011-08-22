package com.codecommit
package es

import errorlist.{DefaultErrorSource, ErrorSource}

import org.gjt.sp.jedit.View

import client._
import util._

class SidekickBackendHandler extends BackendHandler {
  import EnsimeProtocol._
  
  var views = Set[View]()
  
  def backgroundMessage(msg: String) {
    viewMessage("ENSIME: " + msg)
  }
  
  def clearAll() {
    EnsimePlugin.knownErrorSources collect { case src: DefaultErrorSource => src.clear() }
  }
  
  def compilerReady() {
    viewMessage("ENSIME: Compiler ready")
  }
  
  def fullTypecheckFinished() {
    viewMessage("ENSIME: Full typecheck finished")
  }
  
  def indexerReady() {
    viewMessage("ENSIME: Indexer ready")
  }
  
  def error(note: Note) {
    val error = new DefaultErrorSource.DefaultError(_: ErrorSource, ErrorSource.ERROR, note.file, note.line - 1, note.column - 1, note.column - 1 + (note.end - note.begin), note.msg)
    EnsimePlugin.knownErrorSources foreach { src => src.addError(error(src)) }
  }
  
  // hack
  def unhandled(msg: SExp) {}
  
  private def viewMessage(msg: String) {
    views foreach { _.getStatus.setMessage(msg) }
  }
}
