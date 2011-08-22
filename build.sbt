name := "EnsimeSidekick"

version := "0.1"

scalaVersion := "2.9.0-1"

unmanagedJars in Compile += {
  Attributed.blank(new File("/Users/daniel/Library/jEdit/jars/ErrorList.jar"))
}

unmanagedJars in Compile += {
  Attributed.blank(new File("/Users/daniel/Library/jEdit/jars/SideKick.jar"))
}

unmanagedJars in Compile += {
  Attributed.blank(new File("/Applications/jEdit.app/Contents/Resources/Java/jedit.jar"))
}

initialCommands := """
    import com.codecommit.es._
    import java.io.File
    val Cake = new EnsimeProtocolComponent with EnsimeBackendComponent {
      lazy val EnsimeHome = new File("/Users/daniel/Local/ensime_2.9.0-1-0.6.1")
    }
    trait DebugBackendHandler extends BackendHandler {
      def backgroundMessage(msg: String) = println(msg)
      def clearAll() {
        println("Clear all...")
      }
	  def compilerReady() {
	    println("Compiler ready!")
	  }
	  def fullTypecheckFinished() {
	    println("Full typecheck finished")
	  }
	  def indexerReady() {
	    println("Indexer ready!")
	  }
	  def error(note: Note) {
	    printf("[error] %s:%d: %s", note.file, note.line, note.msg)
	  }
      def unhandled(sexp: util.SExp) = println(sexp.toReadableString)
    }
  """
