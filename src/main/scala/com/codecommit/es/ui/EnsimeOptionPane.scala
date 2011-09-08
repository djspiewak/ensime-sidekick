package com.codecommit
package es
package ui

import org.gjt.sp.jedit
import jedit.{jEdit => JEdit, AbstractOptionPane}
import jedit.browser.VFSFileChooserDialog

import java.awt.BorderLayout
import java.awt.event.{ActionEvent, ActionListener}

import javax.swing._

class EnsimeOptionPane extends AbstractOptionPane("ensime") {
  private var homeField = new JTextField(EnsimePlugin.EnsimeHome.getCanonicalPath)
  private var sbtOptsField = new JTextField(EnsimePlugin.SbtOpts)
  
  override def _init() {
    val homePanel = new JPanel(new BorderLayout)
    addComponent("Default ENSIME Home", homePanel)
    
    homePanel.add(homeField)
    
    val button = new JButton("...")
    button.addActionListener(new ActionListener {
      def actionPerformed(e: ActionEvent) {
        val view = JEdit.getActiveView      // can we do without this?
        val dialog = new VFSFileChooserDialog(view, homeField.getText, 0, false, true)
        dialog.getSelectedFiles.headOption foreach homeField.setText
      }
    })
    homePanel.add(button, BorderLayout.EAST)
    
    addComponent("SBT Options", sbtOptsField)
  }
  
  override def _save() {
    JEdit.setProperty(EnsimePlugin.EnsimeHomeProperty, homeField.getText)
    JEdit.setProperty(EnsimePlugin.SbtOptsProperty, sbtOptsField.getText)
  }
}
