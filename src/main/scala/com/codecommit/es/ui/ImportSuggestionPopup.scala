package com.codecommit
package es
package ui

import org.gjt.sp.jedit
import jedit.View
import jedit.gui.CompletionPopup

import java.awt.Point

import javax.swing.{DefaultListCellRenderer, JList}

class ImportSuggestionsPopup(view: View, point: Point, suggestions: Vector[String])(perform: String => Unit) {
  val candidates = new CompletionPopup.Candidates {
    private val renderer = new DefaultListCellRenderer
    
    val getSize = suggestions.length
    val isValid = true
    
    def complete(i: Int) {
      perform(suggestions(i))
    }
    
    def getCellRenderer(list: JList, i: Int, isSelected: Boolean, cellHasFocus: Boolean) =
      renderer.getListCellRendererComponent(list, suggestions(i), i, isSelected, cellHasFocus)
    
    def getDescription(i: Int) = ""
  }
  
  def show() {
    new CompletionPopup(view, point).reset(candidates, true)
  }
}
