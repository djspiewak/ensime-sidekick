package com.codecommit
package es

import scala.util.parsing.input.CharSequenceReader

import util._

trait EnsimeProtocolComponent extends BackendComponent {
  import EnsimeProtocol._
  
  private var returns = Map[Int, SExp => Unit]()
  private val returnLock = new AnyRef
  
  /**
   * Meant to be partially-applied
   */
  def handle(handler: BackendHandler)(chunk: String) {
    val sexp = SExp.read(new CharSequenceReader(chunk))
    sexp match {
      case SExpList(KeywordAtom(":return") :: SExpList(KeywordAtom(":ok") :: inner :: Nil) :: IntAtom(id) :: Nil) => {
        returns get id foreach { _(inner) }
        returnLock synchronized {
          returns -= id
        }
      }
      
      case other => handler.unhandled(other)
    }
  }
  
  lazy val Ensime: Ensime = new Ensime {
    import SExp._
    
    private var _callId = 0
    private val lock = new AnyRef
    
    def connectionInfo(callback: ConnectionInfo => Unit) {
      def parse(sexp: SExp) = sexp match {
        case SExpList(KeywordAtom(":pid") :: pidS :: KeywordAtom(":server-implementation") :: SExpList(KeywordAtom(":name") :: StringAtom(name) :: Nil) :: KeywordAtom(":machine") :: machineS :: KeywordAtom(":features") :: featuresS :: KeywordAtom(":version") :: StringAtom(version) :: Nil) => {
          val pid = pidS match {
            case NilAtom() => None
          }
          val machine = machineS match {
            case NilAtom() => None
          }
          val features = featuresS match {
            case NilAtom() => Nil
          }
          callback(ConnectionInfo(pid, name, machine, features, version))
        }
      }
      
      val id = callId()
      returnLock synchronized {
        returns += (id -> parse)
      }
      dispatchSwank(id, SExpList(Symbol("swank:connection-info") :: Nil))
    }
    
    private def dispatchSwank(id: Int, sexp: SExp) {
      Backend.send(SExpList(key(":swank-rpc") :: sexp :: IntAtom(id) :: Nil).toWireString)
    }
    
    private def callId() = lock synchronized {
      _callId += 1
      _callId
    }
  }
  
  // TODO
  trait Ensime {
    def connectionInfo(callback: ConnectionInfo => Unit)
  }
}

object EnsimeProtocol {
  case class ConnectionInfo(port: Option[Int], name: String, machine: Option[String], features: List[String], version: String)
}
