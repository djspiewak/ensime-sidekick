package com.codecommit
package es
package ui

import com.codecommit.es.client.EnsimeProtocolComponent
import java.awt.EventQueue
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import javax.swing.JTextField
import javax.swing.{JButton, JDialog, JFrame, JLabel, JList, JPanel, JScrollPane, ListSelectionModel, WindowConstants}

import java.awt.{BorderLayout, FlowLayout}
import java.awt.event.{ActionEvent, ActionListener}

class SymbolSearchDialog(parent: JFrame, publicSymbolSearch: (List[String], Int) => (List[(String, String, Int)] => Unit) => Unit) extends JDialog(parent, "Public Symbol Search", true) {
  private var selected: Option[(String, Int)] = None
  private var data = Vector[(String, Int)]()
  
  {
    getContentPane.setLayout(new BorderLayout)
    
    val names = new JTextField
    getContentPane.add(names, BorderLayout.NORTH)
    
    val list = new JList
    list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
    // list.setSelectionInterval(0, 0)
    getContentPane.add(new JScrollPane(list))
    
    names.addKeyListener(new KeyListener {
      var hasOutstanding = false
      var queryNames: Option[List[String]] = None
      
      def keyPressed(e: KeyEvent) {
        if (e.getKeyCode == KeyEvent.VK_ESCAPE) {
          dispose()
        } else if (e.getKeyCode == KeyEvent.VK_ENTER) {
          selected = if (data.isEmpty) {
            None
          } else {
            if (list.getSelectedIndex < 0)
              data.headOption
            else
              Some(data(list.getSelectedIndex))
          }
          
          dispose()
        } else if (e.getKeyCode == KeyEvent.VK_UP) {
          if (list.getSelectedIndex > 0) {
            list.setSelectedIndex(list.getSelectedIndex - 1)
          }
          e.consume()
        } else if (e.getKeyCode == KeyEvent.VK_DOWN) {
          if (list.getSelectedIndex < data.length - 1) {
            list.setSelectedIndex(list.getSelectedIndex + 1)
          }
          e.consume()
        }
      }
      
      def keyReleased(e: KeyEvent) {
      }
      
      def keyTyped(e: KeyEvent) {
        queryNames = Some(names.getText split " " toList)
        if (!hasOutstanding) {
          requestCompletion()
        }
      }
      
      private def requestCompletion() {
        for (tokens <- queryNames) {
          hasOutstanding = true
          queryNames = None
          
          publicSymbolSearch(tokens, 50) { results =>
            val names2 = results map { case (n, _, _) => n }
            val data2 = results map { case (_, f, o) => (f, o) }
            
            EventQueue.invokeLater(new Runnable {
              def run() {
                data = Vector(data2: _*)
                list.setListData(names2.toArray.asInstanceOf[Array[AnyRef]])
              }
            })
            
            hasOutstanding = false
            requestCompletion()
          }
        }
      }
    })
    
    setSize(400, 400)
    
    val screen = getToolkit.getScreenSize
    setLocation((screen.width - getWidth) / 2, (screen.height - getHeight) / 2)
    
    setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)
  }
  
  def open() = {
    setVisible(true)
    selected
  }
}
