package com.codecommit
package es

import errorlist.{DefaultErrorSource, ErrorSource}

import org.gjt.sp.jedit
import jedit.{jEdit => JEdit}
import jedit.{Buffer, EBMessage, EBPlugin, View}
import jedit.msg.ViewUpdate

import java.awt.{EventQueue, Toolkit}
import java.io.File

import javax.swing.JOptionPane

import scala.io.Source
import scala.util.parsing.input.CharSequenceReader

import client._
import util._

class EnsimePlugin extends EBPlugin {
  override def start() {
  }
  
  override def handleMessage(message: EBMessage) = message match {
    case _ =>
  }
  
  override def stop() {
    EnsimePlugin.stopAll()
  }
}

object EnsimePlugin {
  private var instances = Map[File, Instance]()
  private val lock = new AnyRef
  
  def EnsimeHome = new File(Option(JEdit.getProperty(EnsimeHomeProperty)) getOrElse "/Users/daniel/Local/ensime_2.9.1-0.6.RC3")
  
  val EnsimeHomeProperty = "ensime.home"
  
  def stopAll() {
    lock synchronized {
      instances foreach { case (root, inst) =>
        System.err.println("Stopping ENSIME backend at " + root)
        inst.stop()
      }
      instances = instances.empty
    }
  }
  
  def initProject(view: View) {
    // TODO compensate for slow (network) file systems
    val canonicalPath = new File(view.getBuffer.getPath).getCanonicalPath
    val parents = parentDirs(new File(canonicalPath))
    
    if (parents exists instances.contains) {
      view.getStatus.setMessage("ENSIME: Already initialized!")
    } else {
      val projectPath = parents find { f =>
        val files = f.listFiles
        
        if (files != null)
          files filter (null !=) map { _.getName } contains ".ensime"
        else
          false
      } map { f => new File(f, ".ensime") } map { _.getCanonicalPath } getOrElse canonicalPath
      
      EventQueue.invokeLater(new Runnable {
        def run() {
          val projectFileName = JOptionPane.showInputDialog(view, "ENSIME Project File:", projectPath)
          val projectFile = new File(projectFileName)
          
          if (projectFile.exists) {
            val src = Source fromFile projectFile
            val optProjectData = Option(SExp.read(new CharSequenceReader(src.mkString)))
            
            if (!optProjectData.isDefined) {
              JOptionPane.showMessageDialog(view, "Project file is invalid.  Make sure it consists of a single s-expression.  (no comments!)", "Error", JOptionPane.ERROR_MESSAGE)
            }
            
            for (projectData @ SExpList(data) <- optProjectData) {
              val parentDir = projectFile.getParentFile
              val projectData2 = SExpList(SExp.key(":root-dir") :: StringAtom(parentDir.getCanonicalPath) :: data.toList)
              
              val home = projectData2.toKeywordMap get SExp.key(":ensime-home") collect { case StringAtom(str) => str } map { new File(_) } filter { _.exists } getOrElse EnsimeHome
              
              val inst = new Instance(parentDir, home)
              inst.start()
              inst.Ensime.initProject(projectData2) { (projectName, sourceRoots) =>
                lock synchronized {
                  sourceRoots foreach { root => instances += (root -> inst) }
                }
              }
            }
          } else {
            JOptionPane.showMessageDialog(view, "Specified project file does not exist!", "Error", JOptionPane.ERROR_MESSAGE)
          }
        }
      })
    }
  }
  
  def stopProject(view: View) {
    for (inst <- instanceForBuffer(view.getBuffer)) {
      lock synchronized {
        for ((root, `inst`) <- instances) {
          instances -= root
        }
      }
      inst.stop()
      view.getStatus.setMessage("ENSIME: Server instance killed")
    }
  }
  
  def autoComplete(buffer: Buffer, caret: Int, prefix: String)(callback: List[EnsimeProtocol.CompletionResult] => Unit) {
    for (inst <- instanceForBuffer(buffer)) {
      val file = if (buffer.isDirty) {
        buffer.autosave()
        buffer.getAutosaveFile
      } else {
        new File(buffer.getPath)
      }
      
      inst.Ensime.typeCompletion(file.getCanonicalPath, caret, prefix)(callback)
    }
  }
  
  def determineType(view: View) {
    val buffer = view.getBuffer
    
    for (inst <- instanceForBuffer(buffer)) {
      val filename = if (buffer.isDirty) {
        buffer.autosave()
        buffer.getAutosaveFile
      } else {
        new File(buffer.getPath)
      }
      
      inst.Ensime.typeAtPoint(filename.getCanonicalPath, view.getTextArea.getCaretPosition) { t =>
        view.getStatus.setMessage(t.friendlyName)
      }
    }
  }
  
  def typecheckFile(buffer: Buffer) {
    for (inst <- instanceForBuffer(buffer)) {
      if (!buffer.isDirty) {
        inst.Ensime.typecheckFile(new File(buffer.getPath).getCanonicalPath)
      }
    }
  }
  
  def jumpToDeclaration(view: View) {
    val buffer = view.getBuffer
    
    for (inst <- instanceForBuffer(buffer)) {
      val filename = if (buffer.isDirty) {
        buffer.autosave()
        buffer.getAutosaveFile
      } else {
        new File(buffer.getPath)
      }
      
      inst.Ensime.symbolAtPoint(filename.getCanonicalPath, view.getTextArea.getCaretPosition) {
        case Some(loc) => {
          EventQueue.invokeLater(new Runnable {
            def run() {
              type Navigator = { def addToHistory() }
              type NavigatorPlugin = { def getNavigator(view: View): Navigator }
              
              val navPlugin = Option(JEdit.getPlugin("ise.plugin.nav.NavigatorPlugin")) map { _.asInstanceOf[NavigatorPlugin] }
              navPlugin foreach { _.getNavigator(view).addToHistory() }
              
              val buffer = JEdit.openFile(view, loc.file)
              val pane = view.goToBuffer(buffer)
              val area = pane.getTextArea
              area.setCaretPosition(loc.offset)
              
              navPlugin foreach { _.getNavigator(view).addToHistory() }
            }
          })
        }
        
        case None => {
          Toolkit.getDefaultToolkit.beep()
          view.getStatus.setMessage("ENSIME: Could not locate declaration!")
        }
      }
    }
  }
  
  private def instanceForBuffer(buffer: Buffer) =
    parentDirs(new File(buffer.getPath)) flatMap instances.get headOption
  
  private def parentDirs(base: File): Stream[File] =
    base #:: (Option(base.getParent) map { new File(_) } map parentDirs getOrElse Stream.empty)
}

class Instance(val RootDir: File, val EnsimeHome: File) extends EnsimeProtocolComponent with EnsimeBackendComponent {
  val Handler = new SidekickBackendHandler(new DefaultErrorSource("ENSIME"))
  
  def start() {
    ErrorSource.registerErrorSource(Handler.errorSource)
    Backend.start(handle(Handler))
  }
  
  def stop() {
    ErrorSource.unregisterErrorSource(Handler.errorSource)
  }
}
