package org.hello.a

import org.hello.common.WorldCommon

class WorldA {

  def fooA(): String = {
    val s = "a" + new WorldCommon().fooCommon()
    s
  }

  def barA(): String = {
    val s = "a" + new WorldCommon().fooCommon()
    s
  }
}
