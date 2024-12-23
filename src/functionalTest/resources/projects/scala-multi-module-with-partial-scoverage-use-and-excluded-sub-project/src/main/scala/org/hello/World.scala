package org.hello

import org.hello.a.WorldA
import org.hello.a.WorldB

class World {

  def foo(): String = {
    WorldA.foo() + WorldB.foo()
  }
}