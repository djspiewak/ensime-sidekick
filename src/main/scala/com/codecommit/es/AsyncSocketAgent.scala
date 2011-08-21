package com.codecommit
package es

import java.io.{BufferedWriter, InputStreamReader, Reader, OutputStreamWriter}
import java.net.Socket

import java.util.concurrent.{LinkedBlockingQueue, TimeUnit}

private[es] class AsyncSocketAgent(val socket: Socket)(callback: String => Unit) { self =>
  private val queue = new LinkedBlockingQueue[String]
  
  private val writerThread = {
    val back = new Thread {
      override def run() {
        self.runWriter()
      }
    }
    back.setPriority(3)
    back.start()
    back
  }
  
  private val readerThread = {
    val back = new Thread {
      override def run() {
        self.runReader()
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
    
    while (!stopRequested) {
      val work = queue.poll(1, TimeUnit.SECONDS)
      if (work != null) {
        writer.write("%06x" format work.length)
        writer.write(work)
        writer.flush()
      }
    }
  }
  
  // TODO we're just assuming that ENSIME never sends us anything wonky
  private def runReader() {
    val reader = new InputStreamReader(socket.getInputStream)
    
    try {
      while (!stopRequested) {
        val buffer = new Array[Char](readHeader(reader))
        reader.read(buffer)
        callback(new String(buffer))
      }
    } catch { case _ if stopRequested => }
  }
  
  private def readHeader(reader: Reader) = {
    val header = new Array[Char](6)
    reader.read(header)
    Integer.valueOf(new String(header), 16)
  }
}
