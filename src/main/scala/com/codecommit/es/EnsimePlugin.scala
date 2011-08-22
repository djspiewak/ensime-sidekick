package com.codecommit
package es

import org.gjt.sp.jedit
import jedit.{jEdit => JEdit}
import jedit.{EBMessage, EBPlugin, View}
import jedit.msg.ViewUpdate

import java.awt.EventQueue
import java.io.File

import javax.swing.JOptionPane

import client._

class EnsimePlugin extends EBPlugin {
  val handler = new SidekickBackendHandler
  
  override def start() {
    handler.views = Set(JEdit.getViews: _*)
    
    EnsimePlugin.Backend.start(EnsimePlugin.handle(handler))
  }
  
  override def handleMessage(message: EBMessage) = message match {
    case update: ViewUpdate if update.getWhat == ViewUpdate.CREATED =>
      handler.views += update.getView
    
    case update: ViewUpdate if update.getWhat == ViewUpdate.CLOSED =>
      handler.views -= update.getView
    
    case _ =>
  }
  
  override def stop() {
    EnsimePlugin.Backend.stop()
  }
}

object EnsimePlugin extends EnsimeProtocolComponent with EnsimeBackendComponent {
  
  // TODO this should be configurable
  lazy val EnsimeHome = new File("/Users/daniel/Local/ensime_2.9.0-1-0.6.1")
  
  def initProject(path: String) {
    def parentDirs(base: File): Stream[File] =
      base #:: (Option(base.getParent) map { new File(_) } map parentDirs getOrElse Stream.empty)
   
    // TODO compensate for slow (network) file systems
    val canonicalPath = new File(path).getCanonicalPath
    
    val projectPath = parentDirs(new File(canonicalPath)) find { f =>
      val files = f.listFiles
      
      if (files != null)
        files filter (null !=) map { _.getName } contains ".ensime"
      else
        false
    } map { _.getCanonicalPath } getOrElse canonicalPath
    
    Ensime.initProject(JOptionPane.showInputDialog("ENSIME Project Root:", projectPath))
  }
  
  def determineType(view: View) {
    val buffer = view.getBuffer
    
    val filename = if (buffer.isDirty) {
      buffer.autosave()
      buffer.getAutosaveFile
    } else {
      new File(buffer.getPath)
    }
    
    Ensime.typeAtPoint(filename.getCanonicalPath, view.getTextArea.getCaretPosition) { t =>
      view.getStatus.setMessage(t.friendlyName)
    }
  }
}
