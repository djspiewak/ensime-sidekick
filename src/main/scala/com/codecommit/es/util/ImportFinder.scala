package com.codecommit
package es
package util

object ImportFinder {
  val ImportRegex = """\s*import\s+([^;\n\r]+)""".r
  
  def apply(toInsert: String)(lines: Int => Option[String]) = {
    def loop(lineNo: Int, potential: (Int, Boolean)): (Int, Boolean) = {
      val back = lines(lineNo) flatMap { line =>
        val trimmed = line.trim
        
        if (trimmed.startsWith("import")) {
          val ImportRegex(spec) = line
          if (spec < toInsert)
            Some(loop(lineNo + 1, (lineNo + 1, false)))
          else        // spec > toInsert
            Some((lineNo, false))
        } else if (trimmed.startsWith("class")) {
          None
        } else if (trimmed.startsWith("trait")) {
          None
        } else if (trimmed.startsWith("object")) {
          None
        } else if (trimmed.startsWith("private")) {
          None
        } else if (trimmed.startsWith("package object")) {
          None
        } else if (trimmed.startsWith("package")) {
          Some(loop(lineNo + 1, (lineNo + 1, true)))
        } else {
          Some(loop(lineNo + 1, potential))
        }
      }
      
      back getOrElse potential
    }
    
    loop(0, (0, false))
  }
}
