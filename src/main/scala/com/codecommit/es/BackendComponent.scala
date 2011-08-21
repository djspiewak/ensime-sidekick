package com.codecommit
package es

import java.io.File
import java.net.Socket

import scala.io.Source
import scala.sys.process._

trait BackendComponent {
  def Backend: Backend
  
  trait Backend {
    /**
     * Should be idempotent
     */
    def start(callback: String => Unit)
    def stop()
    
    def isStarted: Boolean
    
    def send(chunk: String)
  }
}

trait EnsimeBackendComponent extends BackendComponent {
  def TempDir = new File("/tmp")
  
  def EnsimeHome: File
  
  lazy val Backend = new Backend {
    var isStarted = false
    
    var proc: Process = _
    var portFile: File = _      // ouch!
    var port: Int = -1
    
    var agent: AsyncSocketAgent = _
    
    def start(callback: String => Unit) {
      if (!isStarted) {
        portFile = File.createTempFile("ensime", ".port", TempDir)
        val logFile = File.createTempFile("ensime", ".log", TempDir)
        val serverScript = new File(new File(EnsimeHome, "bin"), "server")
        
        val builder = Process(serverScript.getAbsolutePath + " " + portFile.getCanonicalPath, EnsimeHome)
        proc = (builder #> logFile).run()
        
        while (port < 0) {
          val src = Source fromFile portFile
          src.getLines map { _.toInt } foreach { port = _ }
        }
        
        agent = new AsyncSocketAgent(new Socket("localhost", port))(callback)
        isStarted = true
      }
    }
    
    def send(chunk: String) {
      agent.send(chunk)
    }
    
    def stop() {
      agent.stop()
      Thread.sleep(2000)        // TODO doing something very stupid...
      agent.socket.close()
      
      // kill ensime server by any means necessary
      Process("/bin/sh" :: "-c" :: "kill $(pgrep -f '" + portFile.getCanonicalPath + "')" :: Nil).run()
      proc.destroy()
    }
  }
}
