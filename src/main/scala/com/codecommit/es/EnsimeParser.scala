package com.codecommit
package es

import errorlist.DefaultErrorSource
import sidekick.{SideKickParser, SideKickCompletion, SideKickParsedData}

import org.gjt.sp.jedit
import jedit.{Buffer, EditPane}

import java.io.File

import es.client._

class EnsimeParser extends SideKickParser("ensime") {
  import EnsimeProtocol._
  
  override val canCompleteAnywhere = false
  
  override val supportsCompletion = true
  
  override def complete(editPane: EditPane, caret: Int): SideKickCompletion = {
    val buffer = editPane.getBuffer
    
    val file = if (buffer.isDirty) {
      buffer.autosave()
      buffer.getAutosaveFile
    } else {
      new File(buffer.getPath)
    }
    
    var finalResults: List[CompletionResult] = null
    val signal = new AnyRef
    EnsimePlugin.Ensime.typeCompletion(file.getCanonicalPath, caret, "") { results =>
      finalResults = results
      signal synchronized {
        signal.notifyAll()
      }
    }
    
    signal synchronized {
      signal.wait(5000)
    }
    
    if (finalResults != null) {
      val possibles = finalResults map { _.name: Object } toArray
      
      new SideKickCompletion(editPane.getView, "", possibles) {}
    } else {
      null
    }
  }
  
  // TODO  invoke type check (or something)
  def parse(buffer: Buffer, errorSource: DefaultErrorSource): SideKickParsedData =
    new SideKickParsedData(buffer.getPath)
}
