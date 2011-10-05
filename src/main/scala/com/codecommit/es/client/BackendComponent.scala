package com.codecommit
package es
package client

import java.io.{File, FileOutputStream, InputStream, OutputStream}
import java.net.Socket

import scala.io.Source

trait BackendComponent {
  def Backend: Backend
  
  def fatalServerError(msg: String)
  
  trait Backend {
    
    /**
     * Should be idempotent
     */
    def start(env: (String, String)*)(callback: String => Unit)
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
    
    var stderrCopier: Thread = _
    var stdoutCopier: Thread = _
    
    var agent: AsyncSocketAgent = _
    
    def start(env: (String, String)*)(callback: String => Unit) {
      if (!isStarted) {
        portFile = File.createTempFile("ensime", ".port", TempDir)
        val logFile = File.createTempFile("ensime", ".log", TempDir)
        val serverScript = new File(new File(EnsimeHome, "bin"), "server")
        
        val builder = new ProcessBuilder(serverScript.getAbsolutePath,  portFile.getCanonicalPath)
        
        for ((k, v) <- env) {
          builder.environment.put(k, v)
        }
        
        builder.directory(EnsimeHome)
        proc = builder.start()
        
        val fos = new FileOutputStream(logFile)
        stderrCopier = ioCopier(proc.getErrorStream, fos)
        stdoutCopier = ioCopier(proc.getInputStream, fos)
        
        stderrCopier.start()
        stdoutCopier.start()
        
        // busy-wait until port is written
        while (port < 0) {
          val src = Source fromFile portFile
          src.getLines map { _.toInt } foreach { port = _ }
          src.close()
        }
        
        agent = new AsyncSocketAgent(new Socket("localhost", port), callback, fatalServerError)
        isStarted = true
      }
    }
    
    def send(chunk: String) {
      agent.send(chunk)
    }
    
    def stop() {
      agent.stop()
      agent.socket.close()
      
      stderrCopier.interrupt()
      stdoutCopier.interrupt()
      proc.destroy()
    }
  }
  
  def ioCopier(is: InputStream, os: OutputStream): Thread = new Thread {
    setDaemon(true)
    setPriority(1)
    
    override def run() {
      try {
        var b = is.read()
        while (b >= 0) {
          os.write(b)
          b = is.read()
        }
      } catch {
        case _ => try { os.close() } catch { case _ => }
      }
    }
  }
}
