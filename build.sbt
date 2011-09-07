name := "EnsimeSidekick"

version := "0.1"

scalaVersion := "2.9.1"

unmanagedJars in Compile += {
  Attributed.blank(new File("/Users/daniel/Library/jEdit/jars/ErrorList.jar"))
}

unmanagedJars in Compile += {
  Attributed.blank(new File("/Users/daniel/Library/jEdit/jars/SideKick.jar"))
}

unmanagedJars in Compile += {
  Attributed.blank(new File("/Applications/jEdit.app/Contents/Resources/Java/jedit.jar"))
}
