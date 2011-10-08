package com.codecommit
package es
package client

import java.io.{BufferedWriter, InputStreamReader, OutputStreamWriter, Reader}
import java.net.Socket
import java.util.concurrent.{LinkedBlockingQueue, TimeUnit}
import scala.collection.mutable.ArrayBuffer

private[es] class AsyncSocketAgent(val socket: Socket, callback: String => Unit, failure: String => Unit) { self =>
  private val queue = new LinkedBlockingQueue[String]
  
  private val writerThread = {
    val back = new Thread {
      override def run() {
        try {
          self.runWriter()
        } catch {
          case e => {
            failure("%s: %s".format(e.getClass.getSimpleName, e.toString))
            throw e
          }
        }
      }
    }
    back.setPriority(3)
    back.start()
    back
  }
  
  private val readerThread = {
    val back = new Thread {
      override def run() {
        try {
          self.runReader()
        } catch {
          case e => {
            failure("%s: %s".format(e.getClass.getSimpleName, e.toString))
            throw e
          }
        }
      }
    }
    back.setPriority(3)
    back.start()
    back
  }
  
  private var stopRequested = false
  
  def send(chunk: String) {
    queue.offer(chunk)
  }
  
  def stop() {
    stopRequested = true
  }
  
  private def runWriter() {
    val writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream))
    
    try {
      while (!stopRequested) {
        val work = queue.poll(1, TimeUnit.SECONDS)
        if (work != null) {
          writer.write("%06x" format work.length)
          writer.write(work)
          writer.flush()
        }
      }
    } catch { case _ if stopRequested => }
  }
  
  // TODO we're just assuming that ENSIME never sends us anything wonky
  private def runReader() {
    val reader = new InputStreamReader(socket.getInputStream)
    
    try {
      while (!stopRequested) {
        val totalBuffer = new ArrayBuffer[Char]
        var remaining = readHeader(reader)
        
        while (remaining != 0) {
          val buffer = new Array[Char](remaining)
          remaining -= reader.read(buffer)
          totalBuffer ++= buffer
        }
        
        callback(new String(totalBuffer.toArray))
      }
    } catch { case _ if stopRequested => }
  }
  
  private def readHeader(reader: Reader) = {
    val header = new Array[Char](6)
    reader.read(header)
    Integer.valueOf(new String(header), 16)
  }
}
