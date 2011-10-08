package com.codecommit
package es

import errorlist.DefaultErrorSource
import es.client._
import org.gjt.sp.jedit
import org.gjt.sp.jedit.{Buffer, EditPane}
import sidekick.{SideKickCompletion, SideKickParsedData, SideKickParser}

class EnsimeParser extends SideKickParser("ensime") {
  import EnsimeProtocol._
  import AsyncSocketAgent._
  
  override val canCompleteAnywhere = false
  
  override val supportsCompletion = true
  
  override def complete(editPane: EditPane, caret: Int): SideKickCompletion = {
    System.err.println("Attempting to complete...")
    
    val buffer = editPane.getBuffer
    val optResults = sync(500)(EnsimePlugin.autoComplete(buffer, caret, ""))
    
    optResults map { results =>
      System.err.println("Got results: " + results)
      
      val possibles = results map { _.name: Object } toArray
      
      new SideKickCompletion(editPane.getView, "", possibles) {}
    } getOrElse null
  }
  
  override def parse(buffer: Buffer, errorSource: DefaultErrorSource): SideKickParsedData = {
    EnsimePlugin.typecheckFile(buffer)
    new SideKickParsedData(buffer.getPath)
  }
}
