package org.hello

class World {

  def foo(): String = {
    val s = "a" + "b"
    s
  }

  // not covered by tests
  def bar(): String = "y"
}