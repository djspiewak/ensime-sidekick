package com.codecommit
package es

import scala.util.parsing.input.CharSequenceReader

import util._

trait EnsimeProtocolComponent extends BackendComponent {
  
  /**
   * Meant to be partially-applied
   */
  def handle(handler: BackendHandler)(chunk: String) {
    val sexp = SExp.read(new CharSequenceReader(chunk))
    // TODO parse and invoke specific methods
    handler.unhandled(sexp)
  }
  
  lazy val Ensime: Ensime = new Ensime {
    import SExp._
    
    private var _callId = 0
    private val lock = new AnyRef
    
    def connectionInfo() {
      dispatchSwank(SExpList(Symbol("swank:connection-info") :: Nil))
    }
    
    private def dispatchSwank(sexp: SExp) {
      Backend.send(SExpList(key(":swank-rpc") :: sexp :: IntAtom(callId()) :: Nil).toWireString)
    }
    
    private def callId() = lock synchronized {
      _callId += 1
      _callId
    }
  }
  
  // TODO
  trait Ensime {
    def connectionInfo()
  }
}
