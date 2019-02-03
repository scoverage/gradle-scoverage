package org.hello.b

import org.hello.common.WorldCommon

class WorldB {

  def fooB(): String = {
    val s = "b" + new WorldCommon().fooCommon()
    s
  }
}