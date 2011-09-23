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
    
    var finalResults: List[CompletionResult] = null
    val signal = new AnyRef
    EnsimePlugin.autoComplete(buffer, caret, "") { results =>
      finalResults = results
      signal synchronized {
        signal.notifyAll()
      }
    }
    
    signal synchronized {
      signal.wait(500)
    }
    
    if (finalResults != null) {
      val possibles = finalResults map { _.name: Object } toArray
      
      new SideKickCompletion(editPane.getView, "", possibles) {}
    } else {
      null
    }
  }
  
  def parse(buffer: Buffer, errorSource: DefaultErrorSource): SideKickParsedData = {
    EnsimePlugin.typecheckFile(buffer)
    null
  }
}
