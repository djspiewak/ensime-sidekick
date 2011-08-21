scalaVersion := "2.9.0-1"

unmanagedJars in Compile ++= {
  new File("/Users/daniel/Library/jEdit/jars").listFiles map Attributed.blank toSeq
}
