package com.codecommit
package es
package client

import java.io.File

import scala.collection.mutable
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
    try {
      sexp match {
        case SExpList(KeywordAtom(":return") :: SExpList(KeywordAtom(":ok") :: inner :: Nil) :: IntAtom(id) :: Nil) => {
          returns get id foreach { _(inner) }
          returnLock synchronized {
            returns -= id
          }
        }
        
        case SExpList(KeywordAtom(":return") :: SExpList(KeywordAtom(":abort") :: IntAtom(code) :: StringAtom(detail) :: Nil) :: IntAtom(id) :: Nil) => {
          handler.ensimeError(code, detail)
          if (returns contains id) {
            returnLock synchronized {
              returns -= id
            }
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
        
        case se @ SExpList(KeywordAtom(":scala-notes") :: (props: SExpList) :: Nil) => {
          import SExp._
          
          val map = props.toKeywordMap
          
          val SExpList(notes) = map(key(":notes"))
          for (noteProps @ SExpList(_) <- notes) {
            val noteMap = noteProps.toKeywordMap
            
            val SymbolAtom(severity) = noteMap(key(":severity"))
            
            val StringAtom(msg) = noteMap(key(":msg"))
            val IntAtom(begin) = noteMap(key(":beg"))
            val IntAtom(end) = noteMap(key(":end"))
            val IntAtom(line) = noteMap(key(":line"))
            val IntAtom(column) = noteMap(key(":col"))
            val StringAtom(file) = noteMap(key(":file"))
            
            val note = Note(msg, begin, end, line, column, file)
            
            if (severity == "error")
              handler.error(note)
            else if (severity == "warning")
              handler.warning(note)
          }
        }
        
        case other => handler.unhandled(other)
      }
    } catch {
      case e => e.printStackTrace
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
    
    def initProject(sexp: SExp)(callback: (Option[String], List[File]) => Unit) {
      val id = callId()
      
      registerReturn(id) {
        case results: SExpList => {
          val props = results.toKeywordMap
          
          val projectName = props get key(":project-name") collect { case StringAtom(str) => str }
          val SExpList(rootsSE) = props(key(":source-roots"))
          
          val roots = rootsSE collect { case StringAtom(root) => new File(root) } toList
          
          callback(projectName, roots)
        }
      }
      
      dispatchSwank(id, SExp(key("swank:init-project"), sexp))
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
    
    def typeAtPoint(file: String, offset: Int)(callback: Type => Unit) {
      def parseType(props: SExpList): Type = {
        val map = props.toKeywordMap
        
        val StringAtom(name) = map(key(":name"))
        val IntAtom(id) = map(key(":type-id"))
        val fullName = map get key(":full-name") collect { case StringAtom(s) => s }
        
        val pos = map get key(":pos") collect {
          case props: SExpList => {
            val map = props.toKeywordMap
            
            val StringAtom(file) = map(key(":file"))
            val IntAtom(offset) = map(key(":offset"))
            
            Location(file, offset)
          }
        }
        
        val outerTypeId = map get key(":outer-type-id") collect { case IntAtom(i) => i }
        
        val args = map get key(":type-args") collect {
          case results: SExpList => results collect {
            case props: SExpList => parseType(props)
          } toList
        } getOrElse Nil
        
        Type(name, id, fullName, pos, outerTypeId, args)
      }
      
      val id = callId()
      
      registerReturn(id) {
        case results: SExpList => callback(parseType(results))
      }
      
      dispatchSwank(id, SExp(key("swank:type-at-point"), file, offset))
    }
    
    def typecheckFile(file: String) {
      dispatchSwank(callId(), SExp(key("swank:typecheck-file"), file))
    }
    
    def symbolAtPoint(file: String, offset: Int)(callback: Option[Location] => Unit) {
      val id = callId()
      
      registerReturn(id) {
        case results: SExpList => {
          val map = results.toKeywordMap
          val optDeclPos = map get key(":decl-pos") collect { case se: SExpList => se }
          
          val loc = for (declPosSE <- optDeclPos) yield {
            val declPos = declPosSE.toKeywordMap
            
            val StringAtom(file) = declPos(key(":file"))
            val IntAtom(offset) = declPos(key(":offset"))
            
            Location(file, offset)
          }
          
          callback(loc)
        }
        
        case NilAtom() => callback(None)
      }
      
      dispatchSwank(id, SExp(key("swank:symbol-at-point"), file, offset))
    }
    
    def expandSelection(file: String, start: Int, end: Int)(callback: (Int, Int) => Unit) {
      val id = callId()
      
      registerReturn(id) {
        case results: SExpList => {
          val map = results.toKeywordMap
          
          val IntAtom(start) = map(key(":start"))
          val IntAtom(end) = map(key(":end"))
          
          callback(start, end)
        }
      }
      
      dispatchSwank(id, SExp(key("swank:expand-selection"), file, start, end))
    }
    
    def importSuggestions(file: String, point: Int, names: List[String], maxResults: Int)(callback: List[String] => Unit) {
      val id = callId()
      
      registerReturn(id) {
        case SExpList(outer) => {
          outer foreach {
            case SExpList(possibilities) => {
              val suggestions = possibilities collect {
                case results: SExpList => {
                  val map = results.toKeywordMap
                  val StringAtom(name) = map(key(":name"))
                  name
                }
              }
              
              if (!suggestions.isEmpty) {
                callback(suggestions.toList)
              }
            }
          }
        }
      }
      
      dispatchSwank(id, SExp(key("swank:import-suggestions"), file, point, SExpList(names map StringAtom), maxResults))
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
    def initProject(projectData: SExp)(callback: (Option[String], List[File]) => Unit)
    
    def typeCompletion(file: String, offset: Int, prefix: String)(callback: List[CompletionResult] => Unit)
    def typeAtPoint(file: String, offset: Int)(callback: Type => Unit)
    def typecheckFile(file: String)
    def symbolAtPoint(file: String, offset: Int)(callback: Option[Location] => Unit)
    def expandSelection(file: String, start: Int, end: Int)(callback: (Int, Int) => Unit)
    def importSuggestions(file: String, point: Int, names: List[String], maxResults: Int)(callback: List[String] => Unit)
  }
}

object EnsimeProtocol {
  case class ConnectionInfo(port: Option[Int], name: String, machine: Option[String], features: List[String], version: String)
  
  case class CompletionResult(name: String, typeSig: String, typeId: Int, isCallable: Boolean)
  
  case class Note(msg: String, begin: Int, end: Int, line: Int, column: Int, file: String)
  
  case class Type(name: String, id: Int, fullName: Option[String], pos: Option[Location], outerTypeId: Option[Int], args: List[Type]) {
    def friendlyName: String =
      "%s%s".format(name, if (args.isEmpty) "" else args map { _.friendlyName } mkString("[", ", ", "]"))
  }
  
  case class Location(file: String, offset: Int)
}
