package com.codecommit
package es

import errorlist.{DefaultErrorSource, ErrorSource}

import org.gjt.sp.jedit
import jedit.{jEdit => JEdit, View}

import client._
import util._

class SidekickBackendHandler(val errorSource: DefaultErrorSource) extends BackendHandler {
  import EnsimeProtocol._
  
  def backgroundMessage(msg: String) {
    viewMessage("ENSIME: " + msg)
  }
  
  def clearAll() {
    errorSource.clear()
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
    errorSource.addError(error(errorSource))
  }
  
  def warning(note: Note) {
    val error = new DefaultErrorSource.DefaultError(_: ErrorSource, ErrorSource.WARNING, note.file, note.line - 1, note.column - 1, note.column - 1 + (note.end - note.begin), note.msg)
    errorSource.addError(error(errorSource))
  }
  
  def unhandled(msg: SExp) {
    System.err.println("Unhandled SExp: %s".format(msg.toReadableString))
  }
  
  private def viewMessage(msg: String) {
    JEdit.getViews foreach { _.getStatus.setMessage(msg) }
  }
}
