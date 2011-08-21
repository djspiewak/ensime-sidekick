package com.codecommit
package es

trait EnsimeProtocolComponent extends BackendComponent {
  
  /**
   * Meant to be partially-applied
   */
  def handle(handler: BackendHandler)(chunk: String) {
  }
  
  def Ensime: Ensime = new Ensime {
    
  }
  
  // TODO
  trait Ensime
}
