package com.codecommit
package es
package client

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
      
      case SExpList(KeywordAtom(":background-message") :: _ :: StringAtom(msg) :: Nil) =>
        handler.backgroundMessage(msg)
      
      case SExpList(KeywordAtom(":clear-all-scala-notes") :: TruthAtom() :: Nil) =>
        handler.clearAll()
      
      case SExpList(KeywordAtom(":compiler-ready") :: TruthAtom() :: Nil) =>
        handler.compilerReady()
      
      case SExpList(KeywordAtom(":full-typecheck-finished") :: TruthAtom() :: Nil) =>
        handler.fullTypecheckFinished()
      
      case SExpList(KeywordAtom(":indexer-ready") :: TruthAtom() :: Nil) =>
        handler.indexerReady()
      
      case other => handler.unhandled(other)
    }
  }
  
  lazy val Ensime: Ensime = new Ensime {
    import SExp._
    
    private var _callId = 0
    private val lock = new AnyRef
    
    def connectionInfo(callback: ConnectionInfo => Unit) {
      val id = callId()
      
      registerReturn(id) {
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
      
      dispatchSwank(id, SExp(key("swank:connection-info")))
    }
    
    def initProject(rootDir: String) {
      dispatchSwank(callId(), SExp(key("swank:init-project"), propList(":root-dir" -> StringAtom(rootDir), ":use-sbt" -> TruthAtom())))
    }
    
    def typeCompletion(file: String, offset: Int, prefix: String)(callback: List[CompletionResult] => Unit) {
      val id = callId()
      
      registerReturn(id) {
        case SExpList(results) => {
          val back = results map {
            case props: SExpList => {
              val map = props.toKeywordMap
              
              val StringAtom(name) = map(key(":name"))
              val StringAtom(typeSig) = map(key(":type-sig"))
              val IntAtom(typeId) = map(key(":type-id"))
              val BooleanAtom(isCallable) = map.getOrElse(key(":is-callable"), TruthAtom())
              
              CompletionResult(name, typeSig, typeId, isCallable)
            }
          }
          
          callback(back.toList)
        }
      }
      
      dispatchSwank(id, SExp(key("swank:type-completion"), file, offset, prefix))
    }
    
    def inspectTypeAtPoint(file: String, offset: Int) {
      dispatchSwank(callId(), SExp("swank:inspect-type-at-point", file, offset))
    }
    
    private def dispatchSwank(id: Int, sexp: SExp) {
      Backend.send(SExp(key(":swank-rpc"), sexp, id).toWireString)
    }
    
    private def callId() = lock synchronized {
      _callId += 1
      _callId
    }
  }
  
  private def registerReturn(id: Int)(f: SExp => Unit) {
    returnLock synchronized {
      returns += (id -> f)
    }
  }
  
  // TODO
  trait Ensime {
    def connectionInfo(callback: ConnectionInfo => Unit)
    def initProject(rootDir: String)
    
    def typeCompletion(file: String, offset: Int, prefix: String)(callback: List[CompletionResult] => Unit)
    def inspectTypeAtPoint(file: String, offset: Int)
  }
}

object EnsimeProtocol {
  case class ConnectionInfo(port: Option[Int], name: String, machine: Option[String], features: List[String], version: String)
  
  case class CompletionResult(name: String, typeSig: String, typeId: Int, isCallable: Boolean)
  
  case class Note(msg: String, begin: Int, end: Int, line: Int, column: Int, file: String)
}
