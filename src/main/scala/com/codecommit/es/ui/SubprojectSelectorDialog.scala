package com.codecommit
package es
package ui

import javax.swing.{JButton, JDialog, JFrame, JLabel, JList, JPanel, JScrollPane, ListSelectionModel, WindowConstants}

import java.awt.{BorderLayout, FlowLayout}
import java.awt.event.{ActionEvent, ActionListener}

class SubprojectSelectorDialog(parent: JFrame, projects: Vector[String]) extends JDialog(parent, "Subproject", true) {
  private var selected: Option[String] = None
  
  {
    getContentPane.setLayout(new BorderLayout)
    
    getContentPane.add(new JLabel("Select the active subproject:"), BorderLayout.NORTH)
    
    val list = new JList(projects map { s => s: AnyRef } toArray)
    list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
    list.setSelectionInterval(0, 0)
    getContentPane.add(new JScrollPane(list))
    
    val buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT))
    getContentPane.add(buttons, BorderLayout.SOUTH)
    
    val cancel = new JButton("Cancel")
    cancel.addActionListener(new ActionListener {
      def actionPerformed(e: ActionEvent) {
        dispose()
      }
    })
    buttons.add(cancel)
    
    val ok = new JButton("OK")
    ok.addActionListener(new ActionListener {
      def actionPerformed(e: ActionEvent) {
        selected = Some(list.getSelectedValue.toString)
        dispose()
      }
    })
    getRootPane.setDefaultButton(ok)
    buttons.add(ok)
    
    setSize(200, 200)
    
    val screen = getToolkit.getScreenSize
    setLocation((screen.width - getWidth) / 2, (screen.height - getHeight) / 2)
    
    setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)
  }
  
  def open() = {
    setVisible(true)
    selected
  }
}
