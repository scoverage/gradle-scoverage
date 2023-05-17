package org.hello

class World3_2 {

  // Scala 3 enum to force Scala 3 (Dotty) compiler
  enum Num(val value: String):
    case Three extends Num("3")
    case Two extends Num("2")

  def foo(): String = {
    val s = Num.Three.value + Num.Two.value
    s
  }
}