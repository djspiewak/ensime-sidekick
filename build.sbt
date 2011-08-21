scalaVersion := "2.9.0-1"

unmanagedJars in Compile ++= {
  new File("/Users/daniel/Library/jEdit/jars").listFiles map Attributed.blank toSeq
}

initialCommands := """
    import com.codecommit.es._
    import java.io.File
    val Cake = new EnsimeProtocolComponent with EnsimeBackendComponent {
      lazy val EnsimeHome = new File("/Users/daniel/Local/ensime_2.9.0-1-0.6.1")
    }
    trait DebugBackendHandler extends BackendHandler {
      def unhandled(sexp: util.SExp) = println(sexp.toReadableString)
    }
  """
